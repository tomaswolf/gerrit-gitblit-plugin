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

import javax.servlet.http.HttpSession;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponseDecorator;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(GerritGitBlitWebApp.class);

	private static final String INSTANCE_ATTRIBUTE = "GerritGitBlitPluginInstance";

	private final Provider<WebSession> gerritSesssion;

	private String pluginInstanceKey;

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

	/**
	 * Sets a unique key that is different for each plugin instance (i.e., different for each load of the plugin.)
	 * 
	 * @param key
	 *            for this plugin instance.
	 */
	public void setPluginInstanceKey(String key) {
		log.info("Instance key = {}", key);
		this.pluginInstanceKey = key;
	}

	public String getPluginInstanceKey() {
		return pluginInstanceKey;
	}

	@Override
	protected IRequestCycleProcessor newRequestCycleProcessor() {
		return new WebRequestCycleProcessor() {

			@Override
			public IRequestTarget resolve(RequestCycle requestCycle, RequestParameters requestParameters) {
				if (requestCycle instanceof WebRequestCycle) {
					resetWicketSessionOnPluginReload((WebRequestCycle) requestCycle);
				}
				// If the user logged out in Gerrit, we must tell GitBlit's Wicket session about it.
				GitBlitWebSession wicketSession = GitBlitWebSession.get();
				if (wicketSession.isLoggedIn() && !gerritSesssion.get().isSignedIn()) {
					wicketSession.replaceSession();
					wicketSession.setUser(null);
				}
				return super.resolve(requestCycle, requestParameters);
			}

			private void resetWicketSessionOnPluginReload(WebRequestCycle requestCycle) {
				WebRequest request = requestCycle.getWebRequest();
				HttpSession realSession = request.getHttpServletRequest().getSession();
				if (realSession != null) {
					Object sessionPluginInstanceKey = realSession.getAttribute(INSTANCE_ATTRIBUTE);
					String currentPluginInstanceKey = getPluginInstanceKey();
					if (sessionPluginInstanceKey instanceof String) {
						if (!sessionPluginInstanceKey.equals(currentPluginInstanceKey)) {
							log.info("Clean up after plugin reload");
							// Plugin was restarted during the session. Wicket has stored unserialized Java objects in this session
							// object. We must remove them, so that Wicket creates a new Wicket session, otherwise we end up with
							// ClassCastExceptions further down the line. We mustn't try to get the session and invalidate or replace
							// it; we must just shoot it dead by removing it from the real HTTP session object.
							for (String name : getSessionStore().getAttributeNames(request)) {
								log.info("Removing {}", name);
								getSessionStore().removeAttribute(request, name);
							}
							// Also remove the session unbinding listener. It's already too late to properly do anything with this
							// orphan object that has a class that nobody knows anymore.
							realSession.removeAttribute("Wicket:SessionUnbindingListener-" + getApplicationKey());
							// The above is a bit very hacky. A Wicket Guru might know a cleaner way, but I don't.
							realSession.setAttribute(INSTANCE_ATTRIBUTE, currentPluginInstanceKey);
						}
					} else {
						realSession.setAttribute(INSTANCE_ATTRIBUTE, currentPluginInstanceKey);
					}
				}
			}

			@Override
			protected IRequestCodingStrategy newRequestCodingStrategy() {
				return new StaticCodingStrategy("summary/", "project/");
			}
		};
	}

}
