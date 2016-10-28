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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.request.WebRequestCodingStrategy;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticCodingStrategy extends WebRequestCodingStrategy {
	private static final Logger log = LoggerFactory.getLogger(StaticCodingStrategy.class);
	private final String[] ignoreResourceUrlPrefixes;

	public StaticCodingStrategy(String... ignoreResourceUrlPrefixes) {
		this.ignoreResourceUrlPrefixes = ignoreResourceUrlPrefixes;
	}

	@Override
	public String rewriteStaticRelativeUrl(String url) {
		// Avoid rewriting of non-static resources
		String[] urlParts = url.split("/");
		if (urlParts[urlParts.length - 1].indexOf('.') < 0 || isMatchingIgnoreUrlPrefixes(url)) {
			return super.rewriteStaticRelativeUrl(url);
		}

		int depth = ((ServletWebRequest) RequestCycle.get().getRequest()).getDepthRelativeToWicketHandler();
		return getRelativeStaticUrl(url, depth);
	}

	private boolean isMatchingIgnoreUrlPrefixes(String url) {
		for (String ignoredUrlPrefix : ignoreResourceUrlPrefixes) {
			if (url.startsWith(ignoredUrlPrefix)) {
				return true;
			}
		}
		return false;
	}

	private String getRelativeStaticUrl(String url, int depth) {
		StringBuilder newUrl = new StringBuilder();
		for (; depth > 0; depth--) {
			newUrl.append("../");
		}
		// Trigger our own StaticResourcesServlet. Gerrit's default handling cannot take over
		// since the standard GitBlit jar does not have its static resources in a /static directory
		// but directly at root.
		newUrl.append("static/");
		newUrl.append(url);

		log.debug("Rewriting URL {} to {}", url, newUrl);

		return newUrl.toString();
	}
}
