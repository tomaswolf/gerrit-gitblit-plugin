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

import com.gitblit.models.UserModel;
import com.gitblit.servlet.RawFilter;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritAuthenticationFilter;

@Singleton
public class WrappedRawFilter extends RawFilter {

	private final GerritAuthenticationFilter gerritAuthFilter;
	private final DynamicItem<WebSession> webSession;

	@Inject
	public WrappedRawFilter(final DynamicItem<WebSession> webSession, final GerritAuthenticationFilter gerritAuthFilter) {
		super();

		this.webSession = webSession;
		this.gerritAuthFilter = gerritAuthFilter;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (gerritAuthFilter.doFilter(webSession, request, response, chain)) {
			if (request instanceof HttpServletRequest) {
				request = new FixedGuiceHttpServletRequest((HttpServletRequest) request);
			}
			super.doFilter(request, response, chain);
		}
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
	 * Super class uses httpRequest.getServletPath() in getFullUrl(), but that returns an empty string. Apparently one doesn't have that path yet in a
	 * filter? Instead of trying to figure out how to determine this path here from the FilterConfig, I've taken the easy route and have hard-coded
	 * it.
	 * <p>
	 * {@link GitBlitServletModule} uses this constant to define the paths for the filter and the servlet.
	 * </p>
	 */
	public static final String SERVLET_RELATIVE_PATH = "raw/";

	@Override
	protected String extractRepositoryName(String url) {
		if (url.startsWith(SERVLET_RELATIVE_PATH)) {
			return super.extractRepositoryName(url.substring(SERVLET_RELATIVE_PATH.length()));
		}
		return super.extractRepositoryName(url);
	}

}
