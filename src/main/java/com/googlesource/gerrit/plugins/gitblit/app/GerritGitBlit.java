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

import com.gitblit.GitBlit;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GerritGitBlit extends GitBlit {

	@Inject
	public GerritGitBlit(IRuntimeManager runtimeManager, IPluginManager pluginManager, INotificationManager notificationManager,
			IUserManager userManager, IAuthenticationManager authenticationManager, IPublicKeyManager publicKeyManager,
			IRepositoryManager repositoryManager, IProjectManager projectManager, IFederationManager federationManager) {
		super(runtimeManager, pluginManager, notificationManager, userManager, authenticationManager, publicKeyManager, repositoryManager,
				projectManager, federationManager);
	}

}
