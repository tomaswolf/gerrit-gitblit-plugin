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
// limitations under the License.package com.googlesource.gerrit.plugins.gitblit;
package com.googlesource.gerrit.plugins.gitblit;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.util.IO;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.MimeUtilFileTypeRegistry;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import eu.medsea.mimeutil.MimeType;

@Singleton
public class StaticResourcesServlet extends HttpServlet {

	private static final long serialVersionUID = 5262736289985705065L;

	/**
	 * The resource must either be in one of the allowed subdirectories, or must match the filename pattern. If neither is true, we return a 404.
	 * There's a whole lot of other stuff there that we don't want to expose.
	 */
	private static final Set<String> ALLOWED_SUBDIRECTORIES = ImmutableSet.of("bootstrap", "flotr2", "fontawesome");
	private static final Pattern ALLOWED_FILE_NAMES = Pattern.compile("^(?:gitblit\\.properties|.*\\.(?:png|css|js|swf))$");

	private final MimeUtilFileTypeRegistry mimeDetector;
	private final File gerritPluginDirectory;

	private final AtomicLong lastModified = new AtomicLong(-1L);

	@Inject
	public StaticResourcesServlet(final MimeUtilFileTypeRegistry mimeDetector, final SitePaths sitePaths) {
		super();
		this.mimeDetector = mimeDetector;
		this.gerritPluginDirectory = sitePaths.plugins_dir;
	}

	@Override
	protected long getLastModified(HttpServletRequest request) {
		long result = lastModified.get();
		if (result < 0) {
			// Simply return the time the plugin was put into Gerrit's plugin directory.
			File[] plugins = gerritPluginDirectory.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File directory, String fileName) {
					return fileName != null && fileName.startsWith("gitblit") && fileName.endsWith(".jar");
				}
			});
			result = 0L;
			if (plugins != null && plugins.length > 0) {
				for (File f : plugins) {
					result = Math.max(result, f.lastModified());
				}
			}
			lastModified.set(result);
		}
		return result;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Extract the filename from the request
		String resourcePath = request.getPathInfo(); // Is already relative to /static!
		if (request.getRequestURI().endsWith("/clippy.swf")) {
			resourcePath = "/clippy.swf";
		}
		if (Strings.isNullOrEmpty(resourcePath)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		// Must not contain any navigation
		String[] segments = resourcePath.substring(1).split("/");
		for (String segment : segments) {
			if (segment.equals("..")) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid path");
				return;
			}
		}
		String fileName = segments[segments.length - 1];
		if (Strings.isNullOrEmpty(fileName)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		// Restrict to the few known subdirectories.
		if (segments.length > 1 && !ALLOWED_SUBDIRECTORIES.contains(segments[0])) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} else if (segments.length == 1 && !ALLOWED_FILE_NAMES.matcher(fileName).matches()) {
			// Only allow the known filetypes: we have only png, css, js, swf, and gitblit.properties.
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		// We just happen to know that all our static data files are small, so it's OK to read them fully into memory
		byte[] bytes = null;
		try (InputStream data = getClass().getResourceAsStream(resourcePath)) {
			if (data == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			bytes = IO.readWholeStream(data, 0).array();
		}
		if (bytes == null) {
			// Should not occur since we don't catch the possible IOException above. Nevertheless, let's be paranoid.
			bytes = new byte[0];
		}
		MimeType mimeType = mimeDetector.getMimeType(fileName, bytes);
		String contentType = mimeType != null ? mimeType.toString() : "application/octet-stream";
		if ("application/octet-stream".equals(contentType)) {
			// Gerrit's httpd daemon does this for js.
			if (fileName.toLowerCase().endsWith(".js")) {
				contentType = "application/javascript";
			} else if (fileName.toLowerCase().endsWith(".css")) {
				contentType = "text/css";
			}
		}
		response.setContentType(contentType);
		long lastModified = getLastModified(request);
		if (lastModified > 0) {
			response.setDateHeader("Last-Modified", lastModified);
		}
		response.setHeader("Content-Length", Integer.toString(bytes.length));
		try (OutputStream out = response.getOutputStream()) {
			out.write(bytes, 0, bytes.length);
		}
	}

}
