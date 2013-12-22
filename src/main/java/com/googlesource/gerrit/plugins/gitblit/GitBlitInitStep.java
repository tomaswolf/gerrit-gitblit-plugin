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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.pgm.init.InitStep;
import com.google.gerrit.pgm.init.Section;
import com.google.gerrit.pgm.init.Section.Factory;
import com.google.gerrit.pgm.util.ConsoleUI;
import com.google.inject.Inject;

public class GitBlitInitStep implements InitStep {
  private final ConsoleUI ui;
  private final String pluginName;
  private final Factory sections;

  @Inject
  public GitBlitInitStep(final ConsoleUI ui, final Section.Factory sections,
      @PluginName final String pluginName) {
    this.ui = ui;
    this.pluginName = pluginName;
    this.sections = sections;
  }

  @Override
  public void run() throws Exception {
    ui.message("\n");
    ui.header("GitBlit Integration");

    if(ui.yesno(true, "Do you want to use GitBlit as your GitWeb viewer ?")) {
      configureGitBlit();
    }
  }

  private void configureGitBlit() {
    Section gitWeb = sections.get("gitweb", null);
    gitWeb.set("type", "custom");
    gitWeb.set("url", "plugins/");
    gitWeb.set("project", pluginName + "/summary/?r=${project}");
    gitWeb.set("revision", pluginName + "/commit/?r=${project}&h=${commit}");
    gitWeb.set("branch", pluginName + "/log/?r=${project}&h=${branch}");
    gitWeb.set("filehistory", pluginName + "/history/?f=${file}&r=${project}&h=${branch}");
    gitWeb.string("Link name", "linkname", "GitBlit");
  }

  @Override
  public void postRun() throws Exception {
  }
}
