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
package com.googlesource.gerrit.plugins.gitblit.app;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponseDecorator;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;

import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.GitBlitWebSession;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GerritGitBlitWebApp extends GitBlitWebApp {

	private final Provider<WebSession> gerritSesssion;

	@Inject
	public GerritGitBlitWebApp(IRuntimeManager runtimeManager, IPluginManager pluginManager, INotificationManager notificationManager,
			IUserManager userManager, IAuthenticationManager authenticationManager, IPublicKeyManager publicKeyManager, IRepositoryManager repositoryManager,
			IProjectManager projectManager, IFederationManager federationManager, IGitblit gitblit, Provider<WebSession> gerritSession) {
		super(runtimeManager, pluginManager, notificationManager, userManager, authenticationManager, publicKeyManager, repositoryManager, projectManager,
				federationManager, gitblit);
		this.gerritSesssion = gerritSession;
		// We need this, otherwise the flotr2 library adds again links that are not recoded for static access.
		setHeaderResponseDecorator(new IHeaderResponseDecorator() {
			@Override
			public IHeaderResponse decorate(IHeaderResponse response) {
				return new StaticRewritingHeaderResponse(response);
			}
		});
	}

	@Override
	protected IRequestCycleProcessor newRequestCycleProcessor() {
		return new WebRequestCycleProcessor() {

			@Override
			public IRequestTarget resolve(RequestCycle requestCycle, RequestParameters requestParameters) {
				// If the user logged out in Gerrit, we must tell GitBlit's Wicket session about it.
				GitBlitWebSession wicketSession = GitBlitWebSession.get();
				if (wicketSession.isLoggedIn() && !gerritSesssion.get().isSignedIn()) {
					wicketSession.replaceSession();
					wicketSession.setUser(null);
				}
				return super.resolve(requestCycle, requestParameters);
			}

			@Override
			protected IRequestCodingStrategy newRequestCodingStrategy() {
				return new StaticCodingStrategy("summary/", "project/");
			}
		};
	}

}
