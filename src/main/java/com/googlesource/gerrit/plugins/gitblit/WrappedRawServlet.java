package com.googlesource.gerrit.plugins.gitblit;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import com.gitblit.servlet.RawServlet;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WrappedRawServlet extends RawServlet {
	private static final long serialVersionUID = -3566561701268611822L;

	@Inject
	public WrappedRawServlet() {
		super();
	}

	@Override
	protected String getPath(String repository, String branch, HttpServletRequest request) {
		// Fix a bug in GitBlit. It URL-encodes the resource path, but neglects to decode.
		String base = repository + "/" + branch;
		String path = request.getPathInfo().substring(1);
		if (path.equals(base)) {
			return "";
		}
		// XXX This is specific to GitBlit 1.6.0. Will need revisiting if we upgrade to a newer GitBlit, as the corresponding
		// code on master has changed. Newer GitBlit doesn't URL-encode anymore but does other hacks replacing / itself.
		// I'm sick of all this hand-crafted URL-mangling, done a little differently in all these servlets.
		try {
			path = URLDecoder.decode(path.substring(path.indexOf(base) + base.length() + 1), Charsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			// Cannot handle properly. I'd like to send back a 500. But Java should always know UTF-8.
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	// Note: GitBlit 1.6.0 has another bug in its private method processRequests, but fixing that would require
	// us to override the whole doGet() method. (Bug is line 177, offset += slash;. Should be offset = slash + 1;)
	// See also https://github.com/gitblit/gitblit/commit/e7e8bd8af341719b7cb902c7861ea198f5db43a6
	//
	// With this bug, it only works for repositories directly in the git repo folder, but since we don't have any
	// nested folders with repos there, that doesn't bother us for now.
}
