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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jgit.lib.Config;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.google.common.base.Charsets;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.GitBlitUrlsConfig;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritGitBlitUserManager;

@Singleton
public class GitBlitSettings extends IStoredSettings {
	private static final String GERRIT_GITBLIT_PROPERTIES = "/gitblit.properties";
	private static final String GERRIT_GITBLIT_PROPERTY_SOURCE_KEY = "gerrit_gitblit.property_source";
	private static final String GITBLIT_DIR = "gitblit";

	private final File homeDir;

	private final Properties properties = new Properties();

	@Inject
	public GitBlitSettings(final LocalDiskRepositoryManager repoManager, final @GerritServerConfig Config config, final SitePaths sitePaths) throws IOException {
		super(GitBlitSettings.class);
		// Give GitBlit its own baseDir, otherwise it'll create subfolders in the git repo directory.
		// Note that if you enable Lucene indexing for GitBlit, it will anyway create a subdirectory
		// in that directory called "lucene". See com.gitblit.service.LuceneService. But at least we
		// can keep GitBlit's plugins and tickets directories out of the way.
		this.homeDir = ensureBaseDir(sitePaths.etc_dir);
		load(properties, sitePaths.etc_dir, new GitBlitUrlsConfig(config), repoManager.getBasePath());
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

	/**
	 * Returns either a {@link #GITBLIT_DIR} subdirectory of {@code basePath} or {@code basePath} itself if such a directory doesn't exist and cannot be
	 * created.
	 * 
	 * @param basePath
	 *            to use for GitBlit's base path
	 * @return the directory to use as GitBlit's base path.
	 */
	protected File ensureBaseDir(final File basePath) {
		File dir = new File(basePath, GITBLIT_DIR);
		if (dir.isDirectory() || !dir.exists() && dir.mkdir()) {
			return dir;
		} else {
			return basePath;
		}
	}

	/**
	 * Loads the properties from the given {@link InputStream} using the UTF-8 character encoding. The stream is closed in all cases. A no-op if the stream is
	 * {@code null}.
	 * 
	 * @param properties
	 *            to load; must not be {@code null}
	 * @param stream
	 *            to load from; may be {@code null}
	 * @throws IOException
	 *             if the stream cannot be read.
	 */
	protected void loadFromStream(final Properties properties, final InputStream stream) throws IOException {
		if (stream != null) {
			try (Reader reader = new InputStreamReader(stream, Charsets.UTF_8)) {
				properties.load(reader);
			}
		}
	}

	/**
	 * Loads the properties from the user-supplied gitblit.properties in $GERRIT_SITE/etc, if it exists, and then overwrites with the built-in properties to
	 * ensure that GitBlit is set up as a viewer only and uses Gerrit's git repositories.
	 * 
	 * @param properties
	 *            to load; must not be {@code null}
	 * @param directory
	 *            to look in for the user-supplied file; must not be {@code null}
	 * @param config
	 *            to read Gerrit's URL config from; must not be {@code null}
	 * @param gerritGitDirectory
	 *            Gerrit's git repository directory; must not be {@code null}
	 * @throws IOException
	 *             if the properties cannot be loaded.
	 */
	protected void load(final Properties properties, final File directory, GitBlitUrlsConfig config, File gerritGitDirectory) throws IOException {
		// First, try to load from the user-supplied file, if any.
		File userProperties = new File(directory, GERRIT_GITBLIT_PROPERTIES);
		try {
			loadFromStream(properties, new FileInputStream(userProperties));
			// Record the fact that we loaded the user settings in the properties themselves so that toString() can show it. Useful for debugging.
			properties.put(GERRIT_GITBLIT_PROPERTY_SOURCE_KEY, userProperties.getAbsolutePath());
		} catch (FileNotFoundException ex) {
			// Silently ignore.
		}
		// Remember this key, since we allow overriding the built-in configuration for this one.
		final Object authenticationRequired = properties.get(Keys.web.authenticateViewPages);
		// Override with the built-in viewer-only configuration
		InputStream stream = getClass().getResourceAsStream(GERRIT_GITBLIT_PROPERTIES);
		if (stream == null) {
			throw new IllegalStateException("Built-in configuration " + GERRIT_GITBLIT_PROPERTIES + " cannot be found");
		}
		loadFromStream(properties, stream);
		if (authenticationRequired != null) {
			properties.put(Keys.web.authenticateViewPages, authenticationRequired);
		}
		// Finally some built-in defaults that depend on the Gerrit configuration
		properties.put(Keys.git.repositoriesFolder, gerritGitDirectory.getAbsolutePath());
		properties.put(Keys.realm.userService, GerritGitBlitUserManager.class.getName());
		String gerritDefaultUrls = (config.getGitHttpUrl() + ' ' + config.getGitSshUrl()).trim();
		if (properties.get(Keys.web.otherUrls) != null) {
			properties.put(Keys.web.otherUrls, gerritDefaultUrls + ' ' + properties.get(Keys.web.otherUrls));
		} else {
			properties.put(Keys.web.otherUrls, gerritDefaultUrls);
		}
	}

	public File getBasePath() {
		return homeDir;
	}

	@Override
	public String toString() {
		return properties.toString();
	}

	@Override
	public boolean saveSettings() {
		// We might even consider updating the user-supplied file, if any...
		return false;
	}
}
