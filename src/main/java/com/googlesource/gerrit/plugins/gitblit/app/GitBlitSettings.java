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
package com.googlesource.gerrit.plugins.gitblit.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitblit.GitBlitUrlsConfig;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritToGitBlitUserService;

public class GitBlitSettings extends IStoredSettings {
  private static final String GITBLIT_GERRIT_PROPERTIES = "/gitblit.properties";

  private final LocalDiskRepositoryManager repoManager;
  private final GitBlitUrlsConfig config;
  private final File etcDir;

  private Properties properties;
  private File gitblitPropertiesFile;

  @Inject
  public GitBlitSettings(final LocalDiskRepositoryManager repoManager,
      final @GerritServerConfig Config config, final SitePaths sitePaths)
      throws IOException {
    super(GitBlitSettings.class);
    this.properties = new Properties();
    this.repoManager = repoManager;
    this.config = new GitBlitUrlsConfig(config);
    this.etcDir = sitePaths.etc_dir;
    load();
  }

  @Override
  protected Properties read() {
    return properties;
  }

  @Override
  public boolean saveSettings(Map<String, String> updatedSettings) {
    properties.putAll(updatedSettings);
    return true;
  }

  private void load() throws IOException {
    InputStream resin = openPropertiesFile();
    try {
      properties = new Properties();
      properties.load(resin);
      properties.put("git.repositoriesFolder", repoManager.getBasePath()
          .getAbsolutePath());
      properties.put("realm.userService",
          GerritToGitBlitUserService.class.getName());
      if (properties.get("web.otherUrls") != null) {
        properties.put("web.otherUrls",
            (config.getGitHttpUrl() + " " + config.getGitSshUrl()).trim() + " "
                + properties.get("web.otherUrls"));
      } else {
        properties.put("web.otherUrls",
            (config.getGitHttpUrl() + " " + config.getGitSshUrl()).trim());
      }
    } finally {
      resin.close();
    }
  }

  private InputStream openPropertiesFile() {
    InputStream gitblitPropertiesIn;
    gitblitPropertiesFile = new File(etcDir, GITBLIT_GERRIT_PROPERTIES);
    if (gitblitPropertiesFile.exists()) {
      try {
        gitblitPropertiesIn = new FileInputStream(gitblitPropertiesFile);
      } catch (FileNotFoundException e) {
        // this would never happen as we checked for file existence before
        throw new IllegalStateException(e);
      }
    } else {
      gitblitPropertiesIn =
          getClass().getResourceAsStream(GITBLIT_GERRIT_PROPERTIES);
    }
    return gitblitPropertiesIn;
  }

  public File getBasePath() {
    return repoManager.getBasePath();
  }

  @Override
  public String toString() {
    StringBuilder stringSettings = new StringBuilder();
    if (!gitblitPropertiesFile.exists()) {
      stringSettings.append(GITBLIT_GERRIT_PROPERTIES
          + " from gitblit plugin jar");
    } else {
      stringSettings.append(gitblitPropertiesFile.getAbsolutePath()
          + " (lastModified: "
          + new SimpleDateFormat().format(new Date(gitblitPropertiesFile
              .lastModified())) + ")");
    }

    stringSettings.append(" with values ");
    stringSettings.append(properties.toString());
    return stringSettings.toString();
  }
}
