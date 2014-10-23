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

import java.util.Arrays;
import java.util.List;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;

@Listen
public class GitBlitTopMenu implements TopMenu {
	private final MenuEntry fullMenuEntries;
	private final MenuEntry restrictedMenuEntries;
	private final Provider<CurrentUser> userProvider;

	@Inject
	public GitBlitTopMenu(final @PluginName String pluginName, final Provider<CurrentUser> userProvider) {
		this.userProvider = userProvider;

		String gitBlitBaseUrl = "/plugins/" + pluginName + "/";
		this.restrictedMenuEntries = menu("Gitblit", item("Repositories", gitBlitBaseUrl + "repositories/"));
		this.fullMenuEntries = menu("GitBlit", item("Repositories", gitBlitBaseUrl + "repositories/"), item("Activity", gitBlitBaseUrl + "activity/"),
				item("Search", gitBlitBaseUrl + "lucene/"));
	}

	private MenuEntry menu(String name, MenuItem... items) {
		return new MenuEntry(name, Arrays.asList(items));
	}

	private MenuItem item(String name, String url) {
		return new MenuItem(name, url, "");
	}

	@Override
	public List<MenuEntry> getEntries() {
		return Arrays.asList(userProvider.get().isIdentifiedUser() ? fullMenuEntries : restrictedMenuEntries);
	}
}
