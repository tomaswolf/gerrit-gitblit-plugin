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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.wicket.protocol.http.IWebApplicationFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.servlet.GitblitContext;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlit;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlitWebApp;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritAuthenticationFilter;

@Singleton
public class GerritWicketFilter extends WicketFilter {
	private static final Logger log = LoggerFactory.getLogger(GerritWicketFilter.class);

	private final Provider<WebSession> webSession;
	@SuppressWarnings("unused")
	// We need Guice to create the GerritGitBlit instance
	private final GerritGitBlit gitBlit;
	private final GerritAuthenticationFilter gerritAuthFilter;
	private final GitblitContext applicationContext;
	private final GerritGitBlitWebApp webApp;

	private String pluginInstanceKey;

	@Inject
	public GerritWicketFilter(final Provider<WebSession> webSession, final GerritGitBlit gitBlit, final GitblitContext context,
			final GerritAuthenticationFilter gerritAuthFilter, final GerritGitBlitWebApp webApp) {
		this.webSession = webSession;
		this.gitBlit = gitBlit;
		this.gerritAuthFilter = gerritAuthFilter;
		this.applicationContext = context;
		this.webApp = webApp;
	}

	/**
	 * Sets a unique key that is different for each plugin instance (i.e., different for each load of the plugin.)
	 * 
	 * @param key
	 *            for this plugin instance.
	 */
	public void setPluginInstanceKey(String key) {
		this.pluginInstanceKey = key;
	}

	public String getPluginInstanceKey() {
		return pluginInstanceKey;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info(Constants.BORDER);
		log.info("Starting GitBlit " + Constants.getVersion());
		log.info(Constants.BORDER);
		try {
			applicationContext.contextInitialized(new ServletContextEvent(filterConfig.getServletContext()));
			super.init(new CustomFilterConfig(filterConfig));
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected IWebApplicationFactory getApplicationFactory() {
		return new IWebApplicationFactory() {
			@Override
			public WebApplication createApplication(WicketFilter filter) {
				webApp.setPluginInstanceKey(getPluginInstanceKey());
				return webApp;
			}
		};
	}

	@Override
	protected ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (gerritAuthFilter.doFilter(webSession, request, response, chain)) {
			super.doFilter(request, response, chain);
		}
	}

	class CustomFilterConfig implements FilterConfig {
		private final HashMap<String, String> gitBlitParams = getGitblitInitParams();
		private final FilterConfig parentFilterConfig;

		private HashMap<String, String> getGitblitInitParams() {
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("applicationClassName", GerritGitBlitWebApp.class.getName());
			props.put("filterMappingUrlPattern", "/*");
			props.put("ignorePaths", WrappedPagesFilter.SERVLET_RELATIVE_PATH + ',' + WrappedSyndicationFilter.SERVLET_RELATIVE_PATH + ','
					+ WrappedRawFilter.SERVLET_RELATIVE_PATH);
			return props;
		}

		public CustomFilterConfig(FilterConfig parent) {
			this.parentFilterConfig = parent;
		}

		@Override
		public String getFilterName() {
			return "gerritWicketFilter";
		}

		@Override
		public ServletContext getServletContext() {
			return parentFilterConfig.getServletContext();
		}

		@Override
		public String getInitParameter(String paramString) {
			return gitBlitParams.get(paramString);
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return new Vector<String>(gitBlitParams.keySet()).elements();
		}

		class ParamEnum implements Enumeration<String> {
			Vector<String> items;
			Iterator<String> iter;

			public ParamEnum(Vector<String> items) {
				this.items = items;
				this.iter = this.items.iterator();
			}

			@Override
			public boolean hasMoreElements() {
				return iter.hasNext();
			}

			@Override
			public String nextElement() {
				return iter.next();
			}
		}
	}
}
