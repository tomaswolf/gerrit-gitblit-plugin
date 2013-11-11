//Copyright (C) 2012 The Android Open Source Project
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package com.googlesource.gerrit.plugins.gitblit.app;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.request.WebRequestCodingStrategy;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticCodingStrategy extends WebRequestCodingStrategy {
  private static final Logger LOG = LoggerFactory
      .getLogger(StaticCodingStrategy.class);
  private String[] ignoreResourceUrlPrefixes;

  public StaticCodingStrategy(String... ignoreResourceUrlPrefixes) {
    this.ignoreResourceUrlPrefixes = ignoreResourceUrlPrefixes;
  }

  @Override
  public String rewriteStaticRelativeUrl(String url) {
    // Avoid rewriting of non-static resources
    String[] urlParts = url.split("/");
    if (urlParts[urlParts.length - 1].indexOf('.') < 0) {
      return url;
    }

    if(isMatchingIgnoreUrlPrefixes(url)) {
      return url;
    }

    int depth =
        ((ServletWebRequest) RequestCycle.get().getRequest())
            .getDepthRelativeToWicketHandler();
    return getRelativeStaticUrl(url, depth);
  }

  private boolean isMatchingIgnoreUrlPrefixes(String url) {
    for (String ignoredUrlPrefix : ignoreResourceUrlPrefixes) {
      if(url.startsWith(ignoredUrlPrefix)) {
        return true;
      }
    }
    return false;
  }

  public static String getRelativePrefix(Request request) {
    int depth = ((ServletWebRequest) request).getDepthRelativeToWicketHandler();

    StringBuffer urlBuffer = new StringBuffer();
    for (; depth > 0; depth--) {
      urlBuffer.append("../");
    }

    return urlBuffer.toString();
  }

  public static String getStaticRelativePrefix(Request request) {
    int depth = ((ServletWebRequest) request).getDepthRelativeToWicketHandler();
    return getRelativeStaticUrl("", depth);
  }

  public static String getRelativeStaticUrl(String url, int depth) {
    StringBuffer urlBuffer = new StringBuffer();
    for (; depth > 0; depth--) {
      urlBuffer.append("../");
    }
    urlBuffer.append("static/"); // tells to Gerrit plugin runtime to load
                                 // static resources from gitblit plugin jar
                                 // file
    urlBuffer.append(url);

    LOG.debug("Rewriting URL " + url + " to " + urlBuffer);

    return urlBuffer.toString();
  }
}
