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

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IUserService;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlit;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritToGitBlitUserService;

public class GitBlitServletModule extends ServletModule {
  private static final Logger log = LoggerFactory.getLogger(GitBlitServletModule.class);

  @Inject
  public GitBlitServletModule(@PluginName final String name,
      @GerritServerConfig final Config gerritConfig, final SitePaths sitePaths) {
    log.info("Create GitBlitModule with name='" + name);
  }

  @Override
  protected void configureServlets() {
    log.info("Configuring servlet and filters");
    bind(IUserService.class).to(GerritToGitBlitUserService.class);
    bind(GerritGitBlit.class);

    serve("/pages/*").with(WrappedPagesServlet.class);
    serve("/feed/*").with(WrappedSyndicationServlet.class);
    serve("/zip/*").with(WrappedDownloadZipServlet.class);
    serve("/logo.png").with(WrappedLogoServlet.class);
    serve("/static/logo.png").with(WrappedLogoServlet.class);

    filter("/*").through(GerritWicketFilter.class);
    filter("/pages/*").through(WrappedPagesFilter.class);
    filter("/feed/*").through(WrappedSyndicationFilter.class);
  }
}
