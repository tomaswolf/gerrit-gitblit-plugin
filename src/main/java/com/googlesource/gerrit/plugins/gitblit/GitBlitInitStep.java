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

import org.eclipse.jgit.lib.Config;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.gerrit.pgm.init.api.InitFlags;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.Section;
import com.google.gerrit.pgm.init.api.Section.Factory;
import com.google.inject.Inject;

public class GitBlitInitStep implements InitStep {
	private final ConsoleUI ui;
	private final String pluginName;
	private final Factory sections;
	private final Config cfg;

	@Inject
	public GitBlitInitStep(ConsoleUI ui, Section.Factory sections, @PluginName String pluginName, InitFlags flags) {
		this.ui = ui;
		this.pluginName = pluginName;
		this.sections = sections;
		this.cfg = flags.cfg;
	}

	@Override
	public void run() throws Exception {
		ui.message("\n");
		ui.header("GitBlit Integration");

		if (ui.yesno(true, "Do you want to use GitBlit as your GitWeb viewer?")) {
			configureGitBlit();
		}
		// If we don't use GitBlit here, we leave a potential [plugin "gitblit"] section in the config. It won't hurt,
		// and maybe the user will later re-enable GitBlit, and then he'd be surprised if his customized settings were
		// gone.
	}

	private void configureGitBlit() {
		String defaultLinkName = cfg.getString("gitweb", null, "linkname");
		if (defaultLinkName == null || defaultLinkName.isEmpty()) {
			defaultLinkName = "GitBlit";
		}
		cfg.unsetSection("gitweb", null);
		Section pluginCfg = sections.get("plugin", pluginName);
		// These values are displayed in the UI.
		pluginCfg.string("Link name", "linkname", defaultLinkName, true);
		pluginCfg.string("\"Repositories\" submenu title", "repositories", "Repositories", true);
		pluginCfg.string("\"Activity\" submenu title", "activity", "Activity", true);
		pluginCfg.string("\"Documentation\" submenu title", "documentation", "Documentation", true);
		String originalValue = pluginCfg.get("search");
		if (originalValue == null) {
			pluginCfg.string("\"Search\" submenu title (makes only sense to set if some projects are indexed in GitBlit)", "search", "", true);
		} else {
			String newValue = ui.readString(originalValue, "%s",
					"\"Search\" submenu title (makes only sense to set if some projects are indexed in GitBlit; single dash unsets)");
			if (newValue == null || "-".equals(newValue)) {
				pluginCfg.unset("search");
			} else if (!originalValue.equals(newValue)) {
				pluginCfg.set("search", newValue);
			}
		}
		pluginCfg.unset("browse");
		// If everything is at the default, then make sure we don't have the section at all.
		if (cfg.getNames("plugin", pluginName).isEmpty()) {
			cfg.unsetSection("plugin", pluginName);
		}
	}

	@Override
	public void postRun() throws Exception {
	}
}
