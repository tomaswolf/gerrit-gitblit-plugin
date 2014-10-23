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
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.tickets.ITicketService;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GerritGitBlit extends GitBlit {

	private ITicketService ticketService;

	@Inject
	public GerritGitBlit(IRuntimeManager runtimeManager, IPluginManager pluginManager, INotificationManager notificationManager, IUserManager userManager,
			IAuthenticationManager authenticationManager, IPublicKeyManager publicKeyManager, IRepositoryManager repositoryManager,
			IProjectManager projectManager, IFederationManager federationManager) {
		super(runtimeManager, pluginManager, notificationManager, userManager, authenticationManager, publicKeyManager, repositoryManager, projectManager,
				federationManager);
	}

	@Override
	protected void configureTicketService() {
		// So we inherit a protected method responsible for loading the ticket service... but the corresponding field
		// is private. Oh well. And then the super implementation uses another local dagger injector to dynamically
		// create that class, which just calls the constructor. Could have simply constructed the thing in the super
		// method directly. And on top of that, the "NullTicketService" used by GitBlit is not a "null" implementation
		// but tries to do things. We don't want any of that! Begone!
		ticketService = new ReallyNullTicketService(runtimeManager, pluginManager, notificationManager, userManager, repositoryManager);
		logger.info("Ticket service is disabled");
	}

	@Override
	public ITicketService getTicketService() {
		return ticketService;
	}

	@Override
	public boolean deleteRepositoryModel(RepositoryModel model) {
		// This override is needed mainly because the superclass uses direct field access to its own private ticketService
		// field instead of calling the overridable getTicketService method. Poor design.
		return true;
	}

	@Override
	public void updateRepositoryModel(String repositoryName, RepositoryModel repository, boolean isCreate) throws GitBlitException {
		// Ditto. Intentionally empty.
	}
}
