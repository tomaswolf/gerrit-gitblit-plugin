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

import java.util.Collections;
import java.util.List;

import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.tickets.NullTicketService;
import com.gitblit.tickets.TicketLabel;
import com.gitblit.tickets.TicketMilestone;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link NullTicketService} of GitBlit is not really a null service. It creates a TicketIndexer, and because it neglects to override some methods from its
 * base class, that even gets called sometimes, which then produces exceptions.
 */
@Singleton
public class ReallyNullTicketService extends NullTicketService {

	// Frankly said, what a mess. An abstract super class called ITicketsService (you'd think it was an interface).
	// And then its constructor unconditionally creates a TicketIndexer. And then NullTicketService doesn't override
	// all methods. Bad design.

	@Inject
	public ReallyNullTicketService(IRuntimeManager runtimeManager, IPluginManager pluginManager, INotificationManager notificationManager,
			IUserManager userManager, IRepositoryManager repositoryManager) {
		super(runtimeManager, pluginManager, notificationManager, userManager, repositoryManager);
	}

	@Override
	public boolean hasTickets(RepositoryModel repository) {
		return false;
	}

	@Override
	public boolean isAcceptingNewPatchsets(RepositoryModel repository) {
		return false;
	}

	@Override
	public boolean isAcceptingNewTickets(RepositoryModel repository) {
		return false;
	}

	@Override
	public boolean isAcceptingTicketUpdates(RepositoryModel repository) {
		return false;
	}

	@Override
	public List<TicketLabel> getLabels(RepositoryModel repository) {
		return Collections.emptyList();
	}

	@Override
	public List<TicketMilestone> getMilestones(RepositoryModel repository) {
		return Collections.emptyList();
	}
}
