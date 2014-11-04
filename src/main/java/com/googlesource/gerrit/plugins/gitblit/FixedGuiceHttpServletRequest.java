package com.googlesource.gerrit.plugins.gitblit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * See Guice bug #745: https://github.com/google/guice/issues/745
 * 
 * XXX: remove for Gerrit versions using a Guice version > 3.1.9 (that would mean, when we switch to Gerrit 2.10).
 */
public class FixedGuiceHttpServletRequest extends HttpServletRequestWrapper {

	private final AtomicReference<String> decodedPath;

	private final boolean delegateIsFixed;

	public FixedGuiceHttpServletRequest(HttpServletRequest request) {
		super(request);
		delegateIsFixed = request instanceof FixedGuiceHttpServletRequest;
		decodedPath = delegateIsFixed ? null : new AtomicReference<String>();
	}

	@Override
	public String getPathInfo() {
		if (delegateIsFixed) {
			return super.getPathInfo();
		}
		String result = decodedPath.get();
		if (result == null) {
			result = super.getPathInfo();
			// Guice bug 745 is that this is not decoded, as it should be. Therefore, we need to decode it ourselves, if necessary.
			try {
				result = new URI(result).getPath();
			} catch (URISyntaxException ex) {
				// Ignore and return whatever we got
			}
			decodedPath.set(result);
		}
		return result;
	}
}
