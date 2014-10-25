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

import com.gitblit.servlet.GitblitContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.dagger.GerritDaggerModule;

@Singleton
public class GerritGitBlitContext extends GitblitContext {

	private final GerritDaggerModule guiceBridge;

	@Inject
	public GerritGitBlitContext(final GitBlitSettings settings, final GerritDaggerModule guiceBridge) {
		// Ensure that GitBlit gets initialized in stand-alone mode. Otherwise it uses dagger to create the various
		// managers we need, and also assumes it's running from a war or in some other strange setups.
		super(settings, settings.getBasePath());
		this.guiceBridge = guiceBridge;
	}

	@Override
	protected Object[] getModules() {
		return new Object[] { guiceBridge };
	}

	public void destroy() {
		super.destroyContext(null);
	}
}
