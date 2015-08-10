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

import com.gitblit.Constants.AccessPermission;
import com.gitblit.Constants.AccessRestrictionType;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.UserModel;
import com.gitblit.utils.StringUtils;
import com.google.common.base.Strings;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.RefControl;
import com.google.inject.Provider;

/**
 * A {@link UserModel} that obeys gerrit's access restrictions on repositories and branches.
 */
public class GerritGitBlitUserModel extends UserModel {
	public static final String ANONYMOUS_USER = "$anonymous";
	public static final char[] ANONYMOUS_PASSWORD = ANONYMOUS_USER.toCharArray();

	private static final long serialVersionUID = 1L;

	private transient final ProjectControl.GenericFactory projectControlFactory;
	private transient final Provider<? extends CurrentUser> userProvider;

	public GerritGitBlitUserModel(final ProjectControl.GenericFactory projectControlFactory, final Provider<? extends CurrentUser> userProvider) {
		super(ANONYMOUS_USER);
		this.isAuthenticated = false;
		this.projectControlFactory = projectControlFactory;
		this.userProvider = userProvider;
		this.displayName = this.username;
	}

	public GerritGitBlitUserModel(String username, final ProjectControl.GenericFactory projectControlFactory,
			final Provider<? extends CurrentUser> userProvider) {
		super(username);
		this.username = username;
		this.isAuthenticated = true;
		this.projectControlFactory = projectControlFactory;
		this.userProvider = userProvider;
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
		try {
			ProjectControl control = projectControlFactory.controlFor(new NameKey(StringUtils.stripDotGit(repository.name)), userProvider.get());
			if (control == null) {
				return false;
			}
			switch (ifRestriction) {
			case VIEW:
				return control.isVisible();
			case CLONE:
				return control.canRunUploadPack();
			case PUSH:
				return control.canRunReceivePack();
			default:
				return true;
			}
		} catch (NoSuchProjectException | IOException e) {
			return false;
		}
	}

	@Override
	public boolean hasRepositoryPermission(String name) {
		try {
			ProjectControl control = projectControlFactory.controlFor(new NameKey(StringUtils.stripDotGit(name)), userProvider.get());
			return control != null && control.isVisible();
		} catch (NoSuchProjectException | IOException e) {
			return false;
		}
	}

	@Override
	public boolean canView(RepositoryModel repository, String ref) {
		try {
			ProjectControl control = projectControlFactory.controlFor(new NameKey(StringUtils.stripDotGit(repository.name)), userProvider.get());
			if (control != null && control.isVisible()) {
				RefControl branchCtrl = control.controlForRef(ref);
				return branchCtrl != null && branchCtrl.isVisible();
			}
		} catch (NoSuchProjectException | IOException e) {
			// Silently ignore and return false below.
		}
		return false;
	}

}
