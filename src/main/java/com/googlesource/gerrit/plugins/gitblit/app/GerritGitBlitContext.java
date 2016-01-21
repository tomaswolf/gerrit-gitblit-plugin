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

import javax.servlet.ServletContext;

import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.servlet.GitblitContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GerritGitBlitContext extends GitblitContext {

	private final GitBlitSettings settings;
	private final IRuntimeManager runtime;
	private final INotificationManager notificationManager;
	private final IUserManager userManager;
	private final IAuthenticationManager authenticationManager;
	private final IRepositoryManager repositoryManager;
	private final IProjectManager projectManager;
	private final IGitblit gitblit;

	@Inject
	GerritGitBlitContext(GitBlitSettings settings, IRuntimeManager runtime, INotificationManager notificationManager, IUserManager userManager,
			IAuthenticationManager authenticationManager, IRepositoryManager repositoryManager, IProjectManager projectManager, IGitblit gitblit) {
		this.settings = settings;
		this.runtime = runtime;
		this.notificationManager = notificationManager;
		this.userManager = userManager;
		this.authenticationManager = authenticationManager;
		this.repositoryManager = repositoryManager;
		this.projectManager = projectManager;
		this.gitblit = gitblit;
	}

	@Override
	protected void startCore(ServletContext context) {
		// Manually configure IRuntimeManager
		runtime.setBaseFolder(settings.getBasePath());
		runtime.getStatus().servletContainer = context.getServerInfo();
		runtime.start();
		// Start all other managers
		startManager(notificationManager);
		startManager(userManager);
		startManager(authenticationManager);
		startManager(repositoryManager);
		startManager(projectManager);
		startManager(gitblit);
		logger.info("All Gitblit managers started.");
	}

}
