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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.GitBlit;
import com.gitblit.IStoredSettings;
import com.google.gerrit.common.data.GerritConfig;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlit;
import com.googlesource.gerrit.plugins.gitblit.app.GerritToGitBlitWebApp;
import com.googlesource.gerrit.plugins.gitblit.app.GitBlitSettings;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritAuthFilter;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritToGitBlitUserService;

@Singleton
public class GerritWicketFilter extends WicketFilter {
  private static final String GITBLIT_GERRIT_PROPERTIES = "/gitblit.properties";
  private static final Logger log = LoggerFactory
      .getLogger(GerritWicketFilter.class);

  private final LocalDiskRepositoryManager repoManager;
  private final Provider<WebSession> webSession;
  @SuppressWarnings("unused")
  // We need Guice to create the GerritGitBlit instance
  private final GerritGitBlit gitBlit;
  private final GerritAuthFilter gerritAuthFilter;
  private final GitBlitUrlsConfig config;

  @Inject
  public GerritWicketFilter(final LocalDiskRepositoryManager repoManager,
      final Provider<WebSession> webSession, final GerritGitBlit gitBlit,
      final GerritAuthFilter gerritAuthFilter, final @GerritServerConfig Config config,
      final GerritConfig gerritConfig) {

    this.repoManager = repoManager;
    this.webSession = webSession;
    this.gitBlit = gitBlit;
    this.gerritAuthFilter = gerritAuthFilter;
    this.config = new GitBlitUrlsConfig(config);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    showGitBlitBanner();

    try {
      InputStream resin =
          getClass().getResourceAsStream(GITBLIT_GERRIT_PROPERTIES);
      Properties properties = null;
      try {
        properties = new Properties();
        properties.load(resin);
        properties.put("git.repositoriesFolder", repoManager.getBasePath()
            .getAbsolutePath());
        properties.put("realm.userService",
            GerritToGitBlitUserService.class.getName());
        properties.put("web.otherUrls",
            (config.getGitHttpUrl() + " " + config.getGitSshUrl()).trim());
      } finally {
        resin.close();
      }
      IStoredSettings settings = new GitBlitSettings(properties);
      GitBlit.self().configureContext(settings, repoManager.getBasePath(),
          false);
      GitBlit.self().contextInitialized(
          new ServletContextEvent(filterConfig.getServletContext()));
      super.init(new CustomFilterConfig(filterConfig));
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void showGitBlitBanner() {
    log.info(Constants.BORDER);
    log.info("            _____  _  _    _      _  _  _");
    log.info("           |  __ \\(_)| |  | |    | |(_)| |");
    log.info("           | |  \\/ _ | |_ | |__  | | _ | |_");
    log.info("           | | __ | || __|| '_ \\ | || || __|");
    log.info("           | |_\\ \\| || |_ | |_) || || || |_");
    log.info("            \\____/|_| \\__||_.__/ |_||_| \\__|");
    String submsg = Constants.getGitBlitVersion();
    int spacing = (Constants.BORDER.length() - submsg.length()) / 2;
    StringBuilder sb = new StringBuilder();
    while (spacing > 0) {
      spacing--;
      sb.append(' ');
    }
    log.info(sb.toString() + submsg);
    log.info("");
    log.info(Constants.BORDER);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    if (gerritAuthFilter.doFilter(webSession, request, response, chain)) {
      super.doFilter(request, response, chain);
    }
  }

  class CustomFilterConfig implements FilterConfig {
    private final HashMap<String, String> gitBlitParams = getGitblitInitParams();
    private FilterConfig parentFilterConfig;

    private HashMap<String, String> getGitblitInitParams() {
      HashMap<String, String> props = new HashMap<String, String>();
      props.put("applicationClassName", GerritToGitBlitWebApp.class.getName());
      props.put("filterMappingUrlPattern", "/*");
      props.put("ignorePaths", "pages/,feed/");
      return props;
    }

    public CustomFilterConfig(FilterConfig parent) {
      this.parentFilterConfig = parent;
    }

    public String getFilterName() {
      return "gerritWicketFilter";
    }

    public ServletContext getServletContext() {
      return parentFilterConfig.getServletContext();
    }

    public String getInitParameter(String paramString) {
      return gitBlitParams.get(paramString);
    }

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

      public boolean hasMoreElements() {
        return iter.hasNext();
      }

      public String nextElement() {
        return iter.next();
      }
    }
  }
}
