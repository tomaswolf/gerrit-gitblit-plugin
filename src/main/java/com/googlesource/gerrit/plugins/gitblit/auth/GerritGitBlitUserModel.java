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
package com.googlesource.gerrit.plugins.gitblit.auth;

import java.io.IOException;

import org.eclipse.jgit.errors.ConfigInvalidException;

import com.gitblit.Constants.AccessPermission;
import com.gitblit.Constants.AccessRestrictionType;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.UserModel;
import com.gitblit.utils.StringUtils;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.client.DiffPreferencesInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.GetDiffPreferences;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackend.ForProject;
import com.google.gerrit.server.permissions.PermissionBackend.ForRef;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.inject.Provider;

/**
 * A {@link UserModel} that obeys gerrit's access restrictions on repositories and branches.
 */
public class GerritGitBlitUserModel extends UserModel {
	public static final String ANONYMOUS_USER = "$anonymous";
	public static final char[] ANONYMOUS_PASSWORD = ANONYMOUS_USER.toCharArray();

	private static final long serialVersionUID = 1L;

	private transient final Provider<? extends CurrentUser> userProvider;
	private transient final GetDiffPreferences getDiffPreferences;
	private transient final PermissionBackend permissions;

	public GerritGitBlitUserModel(final PermissionBackend permissions, final Provider<? extends CurrentUser> userProvider,
			final GetDiffPreferences getDiffPreferences) {
		super(ANONYMOUS_USER);
		this.isAuthenticated = false;
		this.permissions = permissions;
		this.userProvider = userProvider;
		this.displayName = this.username;
		this.getDiffPreferences = getDiffPreferences;
	}

	public GerritGitBlitUserModel(String username, final PermissionBackend permissions,
			final Provider<? extends CurrentUser> userProvider, final GetDiffPreferences getDiffPreferences) {
		super(username);
		this.username = username;
		this.isAuthenticated = true;
		this.permissions = permissions;
		this.userProvider = userProvider;
		this.getDiffPreferences = getDiffPreferences;
		CurrentUser user = userProvider.get();
		if (user != null && user.isIdentifiedUser()) {
			this.displayName = ((IdentifiedUser) user).getAccount().getFullName();
			if (Strings.isNullOrEmpty(this.displayName)) {
				this.displayName = this.username;
			}
			this.emailAddress = ((IdentifiedUser) user).getAccount().getPreferredEmail();
		}
	}

	@Override
	protected boolean canAccess(final RepositoryModel repository, final AccessRestrictionType ifRestriction, final AccessPermission requirePermission) {
		ForProject projectPermissions = permissions.user(userProvider).project(new NameKey(StringUtils.stripDotGit(repository.name)));
		if (projectPermissions == null) {
			return false;
		}
		switch (ifRestriction) {
		case VIEW:
			return projectPermissions.testOrFalse(ProjectPermission.ACCESS);
		case CLONE:
			return projectPermissions.testOrFalse(ProjectPermission.RUN_UPLOAD_PACK);
		case PUSH:
			return projectPermissions.testOrFalse(ProjectPermission.RUN_RECEIVE_PACK);
		default:
			return true;
		}
	}

	@Override
	public boolean hasRepositoryPermission(String name) {
		ForProject projectPermissions = permissions.user(userProvider).project(new NameKey(StringUtils.stripDotGit(name)));
		return projectPermissions != null && projectPermissions.testOrFalse(ProjectPermission.ACCESS);
	}

	@Override
	public boolean canView(RepositoryModel repository, String ref) {
		ForProject projectPermissions = permissions.user(userProvider).project(new NameKey(StringUtils.stripDotGit(repository.name)));
		if (projectPermissions != null) {
			ForRef refPermissions = projectPermissions.ref(ref);
			return refPermissions != null && refPermissions.testOrFalse(RefPermission.READ);
		}
		return false;
	}

	/**
	 * Retrieves the Gerrit preference setting for the number of diff context lines. A value < 0 indicates a "full file" context. If the current user
	 * is not logged in, returns the Gitblit (and JGit) default of 3, otherwise the setting as configured by the user in his Gerrit settings.
	 *
	 * @return the number of context lines to display in a diff, or < 0 if the whole file shall be shown.
	 */
	public int diffContext() {
		CurrentUser user = userProvider.get();
		if (user != null && user.isIdentifiedUser()) {
			AccountResource accountRsc = new AccountResource((IdentifiedUser) user);
			try {
				DiffPreferencesInfo diffPrefs = getDiffPreferences.apply(accountRsc);
				if (diffPrefs != null) {
					return diffPrefs.context;
				}
			} catch (AuthException | ConfigInvalidException | PermissionBackendException | IOException e) {
				// Ignore and return default below.
			}
		}
		// This is the DiffFormatter default, and what Gitblit normally uses. The Gerrit default is 10.
		return 3;
	}
}
