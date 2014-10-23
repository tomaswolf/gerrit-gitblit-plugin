// Copyright (C) 2014 The Android Open Source Project
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

import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.html.DecoratingHeaderResponse;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.protocol.http.WebApplication;

import com.google.common.base.Strings;

public class StaticRewritingHeaderResponse extends DecoratingHeaderResponse {

	public StaticRewritingHeaderResponse(final IHeaderResponse real) {
		super(real);
	}

	@Override
	public void renderJavascriptReference(final String url) {
		super.renderJavascriptReference(rewriteUrl(url));
	}

	@Override
	public void renderJavascriptReference(final String url, final String id) {
		super.renderJavascriptReference(rewriteUrl(url), id);
	}

	@Override
	public void renderCSSReference(final String url) {
		super.renderCSSReference(rewriteUrl(url));
	}

	@Override
	public void renderCSSReference(final String url, final String media) {
		super.renderCSSReference(rewriteUrl(url), media);
	}

	private static final Pattern EXTERNAL_URL = Pattern.compile("^(?:\\w+:)?//");

	private String rewriteUrl(final String url) {
		if (Strings.isNullOrEmpty(url) || EXTERNAL_URL.matcher(url).matches()) {
			return url;
		}
		ServletContext servletContext = WebApplication.get().getServletContext();
		String contextPath = servletContext.getContextPath();
		String rewrittenUrl = url;
		if (url.startsWith(contextPath)) {
			if (!url.startsWith(contextPath + "/static/")) {
				rewrittenUrl = contextPath + "/static" + url.substring(contextPath.length());
			}
		} else if (url.charAt(0) == '/') {
			rewrittenUrl = contextPath + "/static" + url;
		} else {
			rewrittenUrl = contextPath + "/static/" + url;
		}
		return rewrittenUrl;
	}
}
