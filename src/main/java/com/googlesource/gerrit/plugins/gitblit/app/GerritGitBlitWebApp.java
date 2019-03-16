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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponseDecorator;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Keys;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.IFilestoreManager;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IServicesManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.tickets.ITicketService;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.gitblit.wicket.CacheControl;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.GitBlitWebSession;
import com.gitblit.wicket.GitblitParamUrlCodingStrategy;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.HttpUtils;

@Singleton
public class GerritGitBlitWebApp extends GitBlitWebApp {
	private static final Logger log = LoggerFactory.getLogger(GerritGitBlitWebApp.class);

	private static final String INSTANCE_ATTRIBUTE = "GerritGitBlitPluginInstance";

	private final DynamicItem<WebSession> gerritSesssion;

	// We have to re-implement this bit from the super class because we have to override mount below because the XSS filtering in the
	// GitblitParamUrlCodingStrategy is wrong (it triggers on perfectly harmless UTF-8 characters like à). Luckily this cacheablePages
	// is not accessed locally in the super class except in two getters, which we also override.
	private final Map<String, CacheControl> cacheablePages = new HashMap<String, CacheControl>();

	/** A key that is unique for each plugin instance. */
	private String pluginInstanceKey;

	@Inject
	public GerritGitBlitWebApp(Provider<IPublicKeyManager> publicKeyManagerProvider, Provider<ITicketService> ticketServiceProvider,
			IRuntimeManager runtimeManager, IPluginManager pluginManager, INotificationManager notificationManager, IUserManager userManager,
			IAuthenticationManager authenticationManager, IRepositoryManager repositoryManager, IProjectManager projectManager,
			IFederationManager federationManager, IGitblit gitblit, IServicesManager services, IFilestoreManager filestoreManager,
			DynamicItem<WebSession> gerritSession) {
		super(publicKeyManagerProvider, ticketServiceProvider, runtimeManager, pluginManager, notificationManager, userManager,
				authenticationManager, repositoryManager, projectManager, federationManager, gitblit, services, filestoreManager);
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
	public void mount(String location, Class<? extends WebPage> clazz, String... parameters) {
		if (parameters == null || !settings().getBoolean(Keys.web.mountParameters, true)) {
			parameters = new String[0];
		}
		mount(new GitblitParamUrlCodingStrategy(settings(), new NullXssFilter(), location, clazz, parameters));

		// map the mount point to the cache control definition
		if (clazz.isAnnotationPresent(CacheControl.class)) {
			CacheControl cacheControl = clazz.getAnnotation(CacheControl.class);
			cacheablePages.put(location.substring(1), cacheControl);
		}
	}

	@Override
	public boolean isCacheablePage(String mountPoint) {
		return cacheablePages.containsKey(mountPoint);
	}

	@Override
	public CacheControl getCacheControl(String mountPoint) {
		return cacheablePages.get(mountPoint);
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
				GitBlitWebSession wicketSession;
				// For some reason that's unclear to me we still sometimes get a ClassCastException here after
				// a plugin reload. Possibly ThreadLocals also survive plugin reload?
				try {
					wicketSession = GitBlitWebSession.get();
				} catch (ClassCastException ex) {
					if (requestCycle instanceof WebRequestCycle) {
						log.info("Force cleanup");
						forceCleanup((WebRequestCycle) requestCycle);
					} else {
						log.warn("Not a web request: {}", requestCycle.getClass().getName());
						Session.unset(); // reset ThreadLocal
					}
					wicketSession = GitBlitWebSession.get();
				}
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
					Object sessionPluginInstanceKey = HttpUtils.getAttribute(realSession, INSTANCE_ATTRIBUTE, String.class);
					String currentPluginInstanceKey = getPluginInstanceKey();
					if (sessionPluginInstanceKey instanceof String) {
						if (!sessionPluginInstanceKey.equals(currentPluginInstanceKey)) {
							cleanUp(realSession, request, currentPluginInstanceKey);
						}
					} else {
						Session.unset();
						realSession.setAttribute(INSTANCE_ATTRIBUTE, currentPluginInstanceKey);
					}
				} else {
					// Should not occur.
					log.warn("No HTTP session");
					Session.unset();
				}
			}

			private void forceCleanup(WebRequestCycle requestCycle) {
				WebRequest request = requestCycle.getWebRequest();
				HttpSession realSession = request.getHttpServletRequest().getSession();
				if (realSession != null) {
					cleanUp(realSession, request, getPluginInstanceKey());
				} else {
					log.warn("No HTTPSession in force cleanup");
					Session.unset();
				}
			}

			private void cleanUp(HttpSession realSession, WebRequest request, String currentPluginInstanceKey) {
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
				Session.unset();
				// The above is a bit very hacky. A Wicket Guru might know a cleaner way, but I don't.
				realSession.setAttribute(INSTANCE_ATTRIBUTE, currentPluginInstanceKey);
			}

			@Override
			protected IRequestCodingStrategy newRequestCodingStrategy() {
				return new StaticCodingStrategy("summary/", "project/");
			}
		};
	}

}
