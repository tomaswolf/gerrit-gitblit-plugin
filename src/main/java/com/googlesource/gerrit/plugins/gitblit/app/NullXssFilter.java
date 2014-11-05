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
package com.googlesource.gerrit.plugins.gitblit.app;

import com.gitblit.utils.XssFilter;

/**
 * This no-op XssFilter is called from GitblitParamUrlCodingStrategy to sanitize URL parameters by removing HTML entities. However, this is the wrong place to
 * attempt XSS prevention. XSS prevention must be done when returning input data to the client, not here. If we do it here, we end up mangling the parameters
 * and then passing them to JGit, which will fail. See https://code.google.com/p/gitblit/issues/detail?id=526 . The correct way to harden GitBlit against XSS
 * attempts would be to use JSoup to generate HTML. (Build the DOM, then serialize it to a string via {@link org.jsoup.nodes.Element#toString()
 * Element.toString()}.)
 * 
 * @author Tom <tw201207@gmail.com>
 */
public class NullXssFilter implements XssFilter {

	@Override
	public String none(String input) {
		return input;
	}

	@Override
	public String relaxed(String input) {
		return input;
	}

}
