// Copyright (C) 2014 Tom <tw201207@gmail.com>
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * See Guice bug #745: https://github.com/google/guice/issues/745
 * 
 * XXX: remove for Gerrit versions using a Guice version >= 4.0-beta5 (that would mean, when we switch to Gerrit 2.10).
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
