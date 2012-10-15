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

import com.gitblit.PagesFilter;
import com.gitblit.models.UserModel;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritAuthFilter;

@Singleton
public class WrappedPagesFilter extends PagesFilter {
  private final GerritAuthFilter gerritAuthFilter;
  private final Provider<WebSession> webSession;

  @Inject
  public WrappedPagesFilter(final Provider<WebSession> webSession, final GerritAuthFilter gerritAuthFilter) {
    super();

    this.webSession = webSession;
    this.gerritAuthFilter = gerritAuthFilter;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    if (gerritAuthFilter.doFilter(webSession, request, response, chain)) {
      super.doFilter(request, response, chain);
    }
  }

  @Override
  protected UserModel getUser(HttpServletRequest httpRequest) {
    UserModel userModel = gerritAuthFilter.getUser(httpRequest);
    if (userModel == null)
      return super.getUser(httpRequest);
    else
      return userModel;
  }
}
