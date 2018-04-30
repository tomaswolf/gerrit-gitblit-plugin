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
package com.googlesource.gerrit.plugins.gitblit.auth;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Keys;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GetDiffPreferences;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.app.GitBlitSettings;

@Singleton
public class GerritGitBlitUserManager implements IUserManager {

	private static final Logger log = LoggerFactory.getLogger(GerritGitBlitUserManager.class);

	private final PermissionBackend permissions;

	private final Provider<CurrentUser> userProvider;

	private final Provider<AnonymousUser> anonymousUser;

	private final GetDiffPreferences getDiffPreferences;

	@Inject
	public GerritGitBlitUserManager(final PermissionBackend permissions, final GitBlitSettings settings,
			final DynamicItem<WebSession> gerritSession, final Provider<AnonymousUser> anonymousUser, final GetDiffPreferences getDiffPreferences) {
		this.permissions = permissions;
		this.userProvider = new Provider<CurrentUser>() {
			@Override
			public CurrentUser get() {
				return gerritSession.get().getUser();
			}
		};
		this.anonymousUser = anonymousUser;
		this.getDiffPreferences = getDiffPreferences;
		if (!settings.getBoolean(Keys.web.authenticateViewPages, false) && !fixAnonymousUser()) {
			settings.saveSettings(ImmutableMap.of(Keys.web.authenticateViewPages, Boolean.TRUE.toString()));
		}
	}

	@Override
	public IUserManager start() {
		return this;
	}

	@Override
	public IUserManager stop() {
		return this;
	}

	@Override
	public void setup(IRuntimeManager runtimeManager) {
	}

	@Override
	public UserModel getUserModel(String username) {
		if (username == null || GerritGitBlitUserModel.ANONYMOUS_USER.equals(username)) {
			return new GerritGitBlitUserModel(permissions, anonymousUser, getDiffPreferences);
		}
		return new GerritGitBlitUserModel(username, permissions, userProvider, getDiffPreferences);
	}

	/**
	 * GitBlit assumes all users (or user accounts) have a username (account name or login name). Gerrit allows users (accounts) to not have a
	 * username, for instance if the account is created or logged in via Google OAuth. I such cases, we have to fake a username for GitBlit.
	 *
	 * @return a GitBlit {@link UserModel} for an unnamed Gerrit account.
	 */
	public UserModel getUnnamedGerritUser() {
		CurrentUser user = userProvider.get();
		if (!user.isIdentifiedUser()) {
			log.warn("\"Logged-in\" user according to session is anonymous.");
			return new GerritGitBlitUserModel(permissions, anonymousUser, getDiffPreferences);
		}
		IdentifiedUser loggedInUser = (IdentifiedUser) user;
		// We know that this user has no username. Synthesize one for GitBlit.
		String fakeUserName = loggedInUser.getAccount().getPreferredEmail();
		if (Strings.isNullOrEmpty(fakeUserName)) {
			fakeUserName = loggedInUser.getAccount().getFullName();
			if (Strings.isNullOrEmpty(fakeUserName)) {
				fakeUserName = "external" + loggedInUser.getAccountId().toString();
			}
		}
		return new GerritGitBlitUserModel(fakeUserName, permissions, userProvider, getDiffPreferences);
	}

	@Override
	public String getCookie(UserModel model) {
		return model.cookie;
	}

	@Override
	public boolean updateUserModel(UserModel model) {
		return false;
	}

	@Override
	public boolean updateUserModel(String username, UserModel model) {
		return false;
	}

	@Override
	public boolean deleteUserModel(UserModel model) {
		return false;
	}

	@Override
	public boolean deleteUser(String username) {
		return false;
	}

	@Override
	public List<String> getAllUsernames() {
		return Collections.emptyList();
	}

	@Override
	public List<UserModel> getAllUsers() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getAllTeamNames() {
		return Collections.emptyList();
	}

	@Override
	public List<TeamModel> getAllTeams() {
		return Collections.emptyList();
	}

	@Override
	public TeamModel getTeamModel(String teamname) {
		return null;
	}

	@Override
	public boolean updateTeamModel(TeamModel model) {
		return false;
	}

	@Override
	public boolean updateTeamModel(String teamname, TeamModel model) {
		return false;
	}

	@Override
	public boolean deleteTeamModel(TeamModel model) {
		return false;
	}

	@Override
	public boolean deleteTeam(String teamname) {
		return false;
	}

	@Override
	public List<String> getUsernamesForRepositoryRole(String role) {
		return Collections.emptyList();
	}

	@Override
	public boolean renameRepositoryRole(String oldRole, String newRole) {
		return false;
	}

	@Override
	public boolean deleteRepositoryRole(String role) {
		return false;
	}

	@Override
	public boolean updateTeamModels(Collection<TeamModel> arg0) {
		return false;
	}

	@Override
	public boolean updateUserModels(Collection<UserModel> arg0) {
		return false;
	}

	@Override
	public UserModel getUserModel(char[] cookie) {
		return null;
	}

	@Override
	public List<String> getTeamNamesForRepositoryRole(String role) {
		return Collections.emptyList();
	}

	@Override
	public boolean isInternalAccount(String username) {
		return false;
	}

	/**
	 * Tries to ensure that GitBlit's "anonymous" user obeys the branch visibility defined by Gerrit.
	 *
	 * @return {@code true} if sucessful, {@code false} if unsuccessful
	 */
	private boolean fixAnonymousUser() {
		// XXX Hack alert!
		//
		// This replaces the static final field UserModel.ANONYMOUS with a new object from our own user model.
		// This may or may not work. The problem here is that that object is hard-coded to the UserModel class and will
		// thus bypass all our Gerrit repository accessibility checks. We do solve this already by overriding a method
		// in GerritGitBlitRepositoryManager, but that class is lacking an operation to determine branch visibility.
		// (The separation of concerns inside GitBlit is a bit haphazard; an operation for branch visibility is on the
		// UserModel, and precisely because of this we need to make sure that the ANONYMOUS object is one of our own
		// user model class.) If GitBlit used Guice and a named Guice injection for this object instead of a static final
		// field, we could solve this much more cleanly by binding it in our module to our own object.
		if (UserModel.ANONYMOUS instanceof GerritGitBlitUserModel) {
			return true;
		}
		try {
			Field anonymousField = UserModel.class.getDeclaredField("ANONYMOUS");
			if (anonymousField != null) {
				anonymousField.setAccessible(true); // Suppress Java-language accessibility checks
				Field modifiers = Field.class.getDeclaredField("modifiers");
				if (modifiers != null) {
					modifiers.setAccessible(true);
					int modifierFlags = anonymousField.getModifiers();
					modifiers.set(anonymousField, modifierFlags & ~Modifier.FINAL); // Remove "final" from the "ANONYMOUS" field
					anonymousField.set(null, new GerritGitBlitUserModel(permissions, anonymousUser, getDiffPreferences));
					modifiers.set(anonymousField, modifierFlags); // Make the field "final" again.
					modifiers.setAccessible(false); // Re-enable Java-language accessibility checks
				}
				anonymousField.setAccessible(false); // Re-enable Java-language accessibility checks
			}
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			if (UserModel.ANONYMOUS instanceof GerritGitBlitUserModel) {
				// Was changed, so the exception occurred later
				log.debug("Reflectively changing the anonymous caused exception after the change", e);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Reflectively changing the anonymous user failed; disabling anonymous access", e);
				} else {
					log.warn("Cannot redefine anonymous user; disabling anonymous access. Error: {}", e.getLocalizedMessage());
				}
			}
		}
		if (UserModel.ANONYMOUS instanceof GerritGitBlitUserModel) {
			log.info("Successfully installed Gerrit anonymous user in GitBlit");
			return true;
		}
		return false;
	}
}
