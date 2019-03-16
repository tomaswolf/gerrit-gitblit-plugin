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

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginCanonicalWebUrl;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GitBlitTopMenu implements TopMenu {

	// Not configurable to avoid mis-configurations clashing with predefined top menus.
	private static final String GITBLIT_TOPMENU_NAME = "GitBlit";

	private final MenuEntry fullMenuEntries;
	private final MenuEntry restrictedMenuEntries;
	private final Provider<CurrentUser> userProvider;

	@Inject
	public GitBlitTopMenu(@PluginName String pluginName, @PluginCanonicalWebUrl String pluginUrl, Provider<CurrentUser> userProvider,
			PluginConfigFactory cfgProvider) {
		this.userProvider = userProvider;

		String gitBlitBaseUrl = pluginUrl.endsWith("/") ? pluginUrl : pluginUrl + '/';
		PluginConfig cfg = cfgProvider.getFromGerritConfig(pluginName, true);
		// We don't have to worry about XSS here; the way these menu item get created through GWT ensures that these values read from the config
		// end up as text nodes in the DOM, even if they contain potentially malicious code. So if somebody sets these values to some HTML snippet,
		// he'll simply end up with a funny looking menu item, but he can't inject things here.
		MenuItem repositories = new MenuItem(cfg.getString("repositories", "Repositories"), gitBlitBaseUrl + "repositories/", "");
		// GitBlit handles its own "/" url, so Gerrit won't produce any link, not even on the "plugins" page, that would display the documentation.
		// I've considered simply redirecting "/" to "/Documentation/" since GitBlit's "/" screen is very similar to its "/activity/" screen, but
		// decided finally to provide an explicit documentation submenu instead.
		MenuItem documentation = new MenuItem(cfg.getString("documentation", "Documentation"), gitBlitBaseUrl + "Documentation/", "");
		restrictedMenuEntries = new MenuEntry(GITBLIT_TOPMENU_NAME, Arrays.asList(repositories, documentation));
		List<MenuItem> fullMenuItems = Lists.newArrayList();
		fullMenuItems.add(repositories);
		fullMenuItems.add(new MenuItem(cfg.getString("activity", "Activity"), gitBlitBaseUrl + "activity/", ""));
		String search = cfg.getString("search");
		if (search != null && !search.isEmpty()) {
			fullMenuItems.add(new MenuItem(search, gitBlitBaseUrl + "lucene/", ""));
		}
		fullMenuItems.add(documentation);
		fullMenuEntries = new MenuEntry(GITBLIT_TOPMENU_NAME, fullMenuItems);
	}

	@Override
	public List<MenuEntry> getEntries() {
		return Arrays.asList(userProvider.get().isIdentifiedUser() ? fullMenuEntries : restrictedMenuEntries);
	}
}
