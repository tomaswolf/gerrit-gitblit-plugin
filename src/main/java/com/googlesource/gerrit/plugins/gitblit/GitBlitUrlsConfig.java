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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class GitBlitUrlsConfig {
  private static final int SSH_DEF_PORT = 22;
  private static final String GITBLIT_REPO = "{0}";
  private static final String GITBLIT_USER = "{1}";
  private static final Logger log = LoggerFactory
      .getLogger(GitBlitUrlsConfig.class);

  private String canonicalWebUrlString;
  private String sshdListenAddressString;
  private String httpdListenUrlString;

  public GitBlitUrlsConfig(Config config) {
    canonicalWebUrlString = config.getString("gerrit", null, "canonicalWebUrl");
    sshdListenAddressString = config.getString("sshd", null, "listenAddress");
    httpdListenUrlString = config.getString("httpd", null, "listenUrl");
  }

  public String getGitSshUrl() {
    if (sshdListenAddressString == null) {
      return "";
    }
    String[] urlParts = sshdListenAddressString.split(":");
    if (urlParts.length < 2) {
      log.error("Invalid SSHD listenUrl: " + sshdListenAddressString);
      return "";
    }
    try {
      String hostname = getHost(urlParts[0]);
      int port = getPort(urlParts[1]);

      return "ssh://" + GITBLIT_USER + "@" + hostname
          + (port == SSH_DEF_PORT ? "" : ":" + port) + "/" + GITBLIT_REPO + "";
    } catch (UnknownHostException e) {
      log.error("Cannot detect localhostname");
      return "";
    }
  }

  private int getPort(String port) {
    return Integer.parseInt(port);
  }

  private String getHost(String hostname) throws UnknownHostException {
    if (hostname.equals("*")) {
      try {
        if (canonicalWebUrlString != null) {
          return new URI(canonicalWebUrlString).getHost();
        }
      } catch (URISyntaxException e) {
        log.error("Cannot parse canonicalWebUrl and get external hostname,"
            + " fallback to auto-detected local hostname", e);
      }
      return InetAddress.getLocalHost().getCanonicalHostName();
    } else {
      return hostname;
    }
  }

  public String getGitHttpUrl() throws UnknownHostException {
    String httpListenUrl = getHttpListenUrl();
    if (httpListenUrl == null) {
      return "";
    }

    String httpUrl = Objects.firstNonNull(canonicalWebUrlString, httpListenUrl);
    httpUrl = httpUrl.replace("://", "://" + GITBLIT_USER + "@");
    httpUrl += (httpUrl.endsWith("/") ? "" : "/") + GITBLIT_REPO;
    return httpUrl;
  }

  private String getHttpListenUrl() throws UnknownHostException {
    if (httpdListenUrlString == null) {
      return null;
    }
    String url = httpdListenUrlString.replaceFirst("proxy-", "");
    if (url.indexOf('*') > 0) {
      url =
          url.replaceFirst("\\*", InetAddress.getLocalHost()
              .getCanonicalHostName());
    }
    return url;
  }
}
