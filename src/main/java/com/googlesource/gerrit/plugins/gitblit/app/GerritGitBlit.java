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
import com.gitblit.GitBlitException;
import com.gitblit.IStoredSettings;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.tickets.NullTicketService;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Singleton
public class GerritGitBlit extends GitBlit {

	@Inject
	public GerritGitBlit(IRuntimeManager runtimeManager, IPluginManager pluginManager, INotificationManager notificationManager, IUserManager userManager,
			IAuthenticationManager authenticationManager, IPublicKeyManager publicKeyManager, IRepositoryManager repositoryManager,
			IProjectManager projectManager, IFederationManager federationManager) {
		super(runtimeManager, pluginManager, notificationManager, userManager, authenticationManager, publicKeyManager, repositoryManager, projectManager,
				federationManager);
	}

	@Override
	public boolean deleteRepositoryModel(RepositoryModel model) {
		// Intentionally empty.
		return true;
	}

	@Override
	public void updateRepositoryModel(String repositoryName, RepositoryModel repository, boolean isCreate) throws GitBlitException {
		// Intentionally empty.
	}

	// We need to provide our own module to ensure the ticket service is really off.
	@Override
	protected Object[] getModules() {
		return new Object[] { new GerritGitBlitModule() };
	}

	// @format:off
	@Module(
		library = true,
		injects = {
			IStoredSettings.class,

			// core managers
			IRuntimeManager.class,
			IPluginManager.class,
			INotificationManager.class,
			IUserManager.class,
			IAuthenticationManager.class,
			IRepositoryManager.class,
			IProjectManager.class,
			IFederationManager.class,

			// the monolithic manager
			IGitblit.class,

			// ticket services
			NullTicketService.class,
			ReallyNullTicketService.class
		}
	)
	// @format:on
	class GerritGitBlitModule {

		@Provides
		@Singleton
		IStoredSettings provideSettings() {
			return settings;
		}

		@Provides
		@Singleton
		IRuntimeManager provideRuntimeManager() {
			return runtimeManager;
		}

		@Provides
		@Singleton
		IPluginManager providePluginManager() {
			return pluginManager;
		}

		@Provides
		@Singleton
		INotificationManager provideNotificationManager() {
			return notificationManager;
		}

		@Provides
		@Singleton
		IUserManager provideUserManager() {
			return userManager;
		}

		@Provides
		@Singleton
		IAuthenticationManager provideAuthenticationManager() {
			return authenticationManager;
		}

		@Provides
		@Singleton
		IRepositoryManager provideRepositoryManager() {
			return repositoryManager;
		}

		@Provides
		@Singleton
		IProjectManager provideProjectManager() {
			return projectManager;
		}

		@Provides
		@Singleton
		IFederationManager provideFederationManager() {
			return federationManager;
		}

		@Provides
		@Singleton
		IGitblit provideGitblit() {
			return GerritGitBlit.this;
		}

		@Provides
		@Singleton
		NullTicketService provideNullTicketService() {
			return new ReallyNullTicketService(runtimeManager, pluginManager, notificationManager, userManager, repositoryManager);
		}

		@Provides
		@Singleton
		ReallyNullTicketService provideReallyNullTicketService() {
			return new ReallyNullTicketService(runtimeManager, pluginManager, notificationManager, userManager, repositoryManager);
		}
	}

}
