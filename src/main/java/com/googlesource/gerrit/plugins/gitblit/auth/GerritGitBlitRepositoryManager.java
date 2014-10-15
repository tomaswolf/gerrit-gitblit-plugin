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
// limitations under the License.package com.googlesource.gerrit.plugins.gitblit;
package com.googlesource.gerrit.plugins.gitblit.auth;

import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.manager.RepositoryManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.UserModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GerritGitBlitRepositoryManager extends RepositoryManager {

	private final IUserManager userManager;

	@Inject
	public GerritGitBlitRepositoryManager(final IRuntimeManager runtimeManager, final IPluginManager pluginManager, final IUserManager userManager) {
		super(runtimeManager, pluginManager, userManager);
		this.userManager = userManager;
	}

	@Override
	public RepositoryModel getRepositoryModel(final UserModel user, final String repositoryName) {
		RepositoryModel repository = getRepositoryModel(repositoryName);
		if (repository != null) {
			// Fix for anonymous access.
			UserModel currentUser = (user instanceof GerritGitBlitUserModel) ? user : userManager.getUserModel(GerritGitBlitUserModel.ANONYMOUS_USER);
			if (currentUser.canView(repository)) {
				return repository;
			}
		}
		return null;
	}

	// Unfortunately, there is no similar method with user, repo, and branch here for branch visibility. That sits over on UserModel, and while we can
	// and do override that one, too, the UserModel.ANONYMOUS is a static final object of type UserClass, which leads to problems to properly implement
	// Gerrit's per-ref access rules that are difficult to overcome.
	//
	// The original Gerrit-GitBlit plugin ensured that view pages always required authentication, and somehow managed to provide a fake "$anonymous"
	// "authenticated" user to GitBlit if the user was not logged in to Gerrit. That fake user was a GerritGitBlitUserModel instance and thus obeyed
	// Gerrit's rules. However, with that approach I had some problems:
	// - it displays "$anonymous" as the username on the UI, which is ugly.
	// - it doesn't really work in all circumstances. I had cases where I had trouble after I had been logged in and then logged out via GitBlit's
	//   logout.
	// - the "$anonymous" user has a "logout" menu item, which just doesn't make sense.
	// - one cannot log in via GitBlit, which makes deep links to GitBlit pages containing things the anonymous user cannot see fail unless the
	//   person who visits the link happens to be logged in in Gerrit already. Which is kind of bad since GitBlit never shows any login form,
	//   since there's always an "authenticated" user, either a real one or this fake "$anonymous" user.
	// - I never got that approach to work with GitBlit 1.6.0 at all, but that is in all likelihood my fault.
	//
	// Because of the above, I've opted for a somewhat different approach. All authentication goes via Gerrit. Log in logs you in to gerrit, log out
	// logs you out from Gerrit. Only real Gerrit users are ever logged in; if you're not logged in, you're really anonymous and not authenticated. I
	// redefine the UserModel.ANONYMOUS object to ensure that it is indeed a GerritGitBlitUserModel that does indeed query Gerrit for access
	// permissions. (Admittedly, that's a gruesome hack. It would have helped if the UserManager had a method getAnonymousUser() and that was used
	// consistently throughout GitBlit instead of these ubiquitous direct field accesses to UserModel.ANONYMOUS. Then I could just have overridden
	// that method.)
	//
	// As a result, login and logout work as expected, and if GitBlit is configured to allow anonymous viewing, non-logged-in (anonymous) users can
	// indeed only see what they're supposed to see and have to log-in otherwise.
}
