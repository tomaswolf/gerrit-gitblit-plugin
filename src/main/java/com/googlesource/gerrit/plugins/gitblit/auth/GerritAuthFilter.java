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
package com.googlesource.gerrit.plugins.gitblit.auth;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.gitblit.GitBlit;
import com.gitblit.models.UserModel;
import com.google.common.base.Objects;
import com.google.gerrit.httpd.WebSession;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GerritAuthFilter {
  private static final String LIT_BASIC = "Basic ";

  /**
   * Returns the user making the request, if the user has authenticated.
   * @param httpRequest
   * @return user
   */
  public UserModel getUser(HttpServletRequest httpRequest) {
    UserModel user = null;
    String username = (String) httpRequest.getAttribute("gerrit-username");
    String token = (String) httpRequest.getAttribute("gerrit-token");

    if (token == null || username == null) {
      return null;
    }

    user =
        GitBlit.self().authenticate(username,
            (GerritToGitBlitUserService.SESSIONAUTH + token).toCharArray());
    if (user != null) {
      return user;
    }

    return null;
  }

  public boolean doFilter(final Provider<WebSession> webSession,
      ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    String hdr = httpRequest.getHeader("Authorization");
    if (hdr != null) {
      return filterBasicAuth((HttpServletRequest) request,
          (HttpServletResponse) response, hdr);
    } else if (webSession.get().isSignedIn()) {
      return filterSessionAuth(webSession, (HttpServletRequest) request);
    } else {
      return true;
    }
  }

  public boolean filterSessionAuth(final Provider<WebSession> webSession,
      HttpServletRequest request) {
    request.setAttribute("gerrit-username", webSession.get().getCurrentUser()
        .getUserName());
    request.setAttribute("gerrit-token", webSession.get().getSessionId());
    return true;
  }

  public boolean filterBasicAuth(HttpServletRequest request,
      HttpServletResponse response, String hdr) throws IOException,
      UnsupportedEncodingException {
    if (!hdr.startsWith(LIT_BASIC)) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"Gerrit Code Review\"");
      response.sendError(SC_UNAUTHORIZED);
      return false;
    }

    final byte[] decoded =
        new Base64().decode(hdr.substring(LIT_BASIC.length()).getBytes());
    String usernamePassword =
        new String(decoded, Objects.firstNonNull(
            request.getCharacterEncoding(), "UTF-8"));
    int splitPos = usernamePassword.indexOf(':');
    if (splitPos < 1) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"Gerrit Code Review\"");
      response.sendError(SC_UNAUTHORIZED);
      return false;
    }
    request.setAttribute("gerrit-username",
        usernamePassword.substring(0, splitPos));
    request.setAttribute("gerrit-password",
        usernamePassword.substring(splitPos + 1));

    return true;
  }

}
