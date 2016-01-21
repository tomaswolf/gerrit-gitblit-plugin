// Copyright (C) 2014 The Android Open Source Project
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.PluginState;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.Version;

import com.gitblit.manager.IManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.models.PluginRegistry.InstallState;
import com.gitblit.models.PluginRegistry.PluginRegistration;
import com.gitblit.models.PluginRegistry.PluginRelease;
import com.google.inject.Singleton;

@Singleton
public class NullPluginManager implements IPluginManager {

	private static final Logger log = LoggerFactory.getLogger(NullPluginManager.class);

	@Override
	public IManager start() {
		log.info("Null plugin manager set up: no plugins in gitblit!");
		return this;
	}

	@Override
	public IManager stop() {
		return this;
	}

	@Override
	public Version getSystemVersion() {
		return Version.ZERO;
	}

	@Override
	public void startPlugins() {
		// Do nothing
	}

	@Override
	public void stopPlugins() {
		// Do nothing
	}

	@Override
	public PluginState startPlugin(String pluginId) {
		return null;
	}

	@Override
	public PluginState stopPlugin(String pluginId) {
		return null;
	}

	@Override
	public List<Class<?>> getExtensionClasses(String pluginId) {
		return Collections.emptyList();
	}

	@Override
	public <T> List<T> getExtensions(Class<T> type) {
		return Collections.emptyList();
	}

	@Override
	public List<PluginWrapper> getPlugins() {
		return Collections.emptyList();
	}

	@Override
	public PluginWrapper getPlugin(String pluginId) {
		return null;
	}

	@Override
	public PluginWrapper whichPlugin(Class<?> clazz) {
		return null;
	}

	@Override
	public boolean disablePlugin(String pluginId) {
		return false;
	}

	@Override
	public boolean enablePlugin(String pluginId) {
		return false;
	}

	@Override
	public boolean uninstallPlugin(String pluginId) {
		return false;
	}

	@Override
	public boolean refreshRegistry(boolean verifyChecksum) {
		return false;
	}

	@Override
	public boolean installPlugin(String url, boolean verifyChecksum) throws IOException {
		return false;
	}

	@Override
	public boolean upgradePlugin(String pluginId, String url, boolean verifyChecksum) throws IOException {
		return false;
	}

	@Override
	public List<PluginRegistration> getRegisteredPlugins() {
		return Collections.emptyList();
	}

	@Override
	public List<PluginRegistration> getRegisteredPlugins(InstallState state) {
		return Collections.emptyList();
	}

	@Override
	public PluginRegistration lookupPlugin(String idOrName) {
		return null;
	}

	@Override
	public PluginRelease lookupRelease(String idOrName, String version) {
		return null;
	}
}
