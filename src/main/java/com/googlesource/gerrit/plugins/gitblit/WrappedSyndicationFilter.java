// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.gitblit;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gitblit.manager.IRepositoryManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.UserModel;
import com.gitblit.servlet.GerritGitBlitAuthenticatedRequest;
import com.gitblit.servlet.SyndicationFilter;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritAuthenticationFilter;

@Singleton
public class WrappedSyndicationFilter extends SyndicationFilter {

	private final GerritAuthenticationFilter gerritAuthFilter;

	private final Provider<WebSession> webSession;

	private final IRepositoryManager repositoryManager;

	@Inject
	public WrappedSyndicationFilter(final Provider<WebSession> webSession, GerritAuthenticationFilter gerritAuthFilter, IRepositoryManager repositoryManager) {
		super();
		this.webSession = webSession;
		this.gerritAuthFilter = gerritAuthFilter;
		this.repositoryManager = repositoryManager;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!gerritAuthFilter.doFilter(webSession, request, response, chain)) {
			return;
		}
		// Code copied from super class.
		//
		// Don't call super, as it doesn't always check the user's access rights. Well, frankly said, I'm not too sure how this RSS
		// stuff is supposed to work. But surely one should not expose repositories not visible by anonymous users via a feed?
		//
		// Also omit all the stuff about GitBlit projects. We use GitBlit only as a viewer, and Gerrit has no such concept. A
		// Gerrit project _is_ a repository.
		HttpServletRequest httpRequest = new FixedGuiceHttpServletRequest((HttpServletRequest) request);
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		String fullUrl = getFullUrl(httpRequest);
		String name = extractRequestedName(fullUrl);

		RepositoryModel model = null;

		// try loading a repository model
		model = repositoryManager.getRepositoryModel(name);
		if (model == null) {
			logger.warn("No repository found for feed {} , sending 404", fullUrl);
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// Wrap the HttpServletRequest with the AccessRestrictionRequest which
		// overrides the servlet container user principal methods.
		// JGit requires either:
		//
		// 1. servlet container authenticated user
		// 2. http.receivepack = true in each repository's config
		//
		// Gitblit must conditionally authenticate users per-repository so just
		// enabling http.receivepack is insufficient.
		GerritGitBlitAuthenticatedRequest authenticatedRequest = new GerritGitBlitAuthenticatedRequest(httpRequest);
		UserModel user = getUser(httpRequest);
		// BASIC authentication challenge and response processing
		if (user == null && !UserModel.ANONYMOUS.canView(model)) {
			// Challenge client to provide credentials. send 401.
			logger.info("CHALLENGE {}", fullUrl);
			httpResponse.setHeader("WWW-Authenticate", CHALLENGE);
			httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		} else if (user != null) {
			authenticatedRequest.setUser(user);
			if (!user.canView(model)) {
				logger.info("Deny access for user {} to feed {}", user.getName(), fullUrl);
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		// Pass processing to the restricted servlet. Note: no ref-level security. I guess we might have to rewrite the whole feed servlet for that.
		newSession(authenticatedRequest, httpResponse);
		logger.info("Authenticated user {} for feed {}", authenticatedRequest.getRemoteUser(), fullUrl);
		chain.doFilter(authenticatedRequest, httpResponse);
	}

	@Override
	protected UserModel getUser(HttpServletRequest httpRequest) {
		UserModel userModel = gerritAuthFilter.getUser(httpRequest);
		if (userModel == null) {
			return super.getUser(httpRequest);
		} else {
			return userModel;
		}
	}

	/**
	 * Super class uses httpRequest.getServletPath() in getFullUrl(), but that returns an empty string. Apparently one doesn't have that path yet in a filter?
	 * Instead of trying to figure out how to determine this path here from the FilterConfig, I've taken the easy route and have hard-coded it.
	 * <p>
	 * {@link GitBlitServletModule} uses this constant to define the paths for the filter and the servlet.
	 * </p>
	 */
	public static final String SERVLET_RELATIVE_PATH = "feed/";

	@Override
	protected String extractRequestedName(String url) {
		String result = super.extractRequestedName(url);
		if (result.startsWith(SERVLET_RELATIVE_PATH)) {
			return result.substring(SERVLET_RELATIVE_PATH.length());
		}
		return result;
	}
}
