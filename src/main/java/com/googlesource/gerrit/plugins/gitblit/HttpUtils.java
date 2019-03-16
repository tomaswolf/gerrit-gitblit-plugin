// Copyright (C) 2019 The Android Open Source Project
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

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HttpUtils {

	// Can't use injection since used in Gitblit wicket pages

	public static <T> T getAttribute(final HttpServletRequest httpRequest, String attribute, Class<T> clazz) {
		Object obj = httpRequest.getAttribute(attribute);
		if (obj instanceof Optional) {
			obj = ((Optional<?>) obj).orElse(null);
		}
		if (clazz.isInstance(obj)) {
			return clazz.cast(obj);
		}
		return null;
	}

	public static <T> T getAttribute(final HttpSession session, String attribute, Class<T> clazz) {
		Object obj = session.getAttribute(attribute);
		if (obj instanceof Optional) {
			obj = ((Optional<?>) obj).orElse(null);
		}
		if (clazz.isInstance(obj)) {
			return clazz.cast(obj);
		}
		return null;
	}

}
