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

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.IUserService;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import com.google.common.base.Strings;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.AuthResult;
import com.google.gerrit.server.project.ProjectControl;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GerritToGitBlitUserService implements IUserService {
  private static final Logger log = LoggerFactory
      .getLogger(GerritToGitBlitUserService.class);

  private final ProjectControl.Factory projectControl;
  private AccountManager accountManager;

  private Provider<WebSession> webSession;

  public static final String SESSIONAUTH = "sessionid:";

  @Inject
  public GerritToGitBlitUserService(
      final ProjectControl.Factory projectControl,
      AccountManager accountManager, final Provider<WebSession> webSession) {
    this.projectControl = projectControl;
    this.accountManager = accountManager;
    this.webSession = webSession;
  }

  @Override
  public UserModel authenticate(String username, char[] password) {
    String passwordString = new String(password);

    if (username.equals(GerritToGitBlitUserModel.ANONYMOUS_USER)) {
      return GerritToGitBlitUserModel.getAnonymous(projectControl);
    } else if (passwordString
        .startsWith(GerritToGitBlitUserService.SESSIONAUTH)) {
      return authenticateSSO(username,
          passwordString.substring(GerritToGitBlitUserService.SESSIONAUTH
              .length()));
    } else {
      return authenticateBasicAuth(username, passwordString);
    }
  }

  public UserModel authenticateSSO(String username, String sessionToken) {
    WebSession session = webSession.get();

    if (session.getSessionId() == null || !session.getSessionId().equals(sessionToken)) {
      log.warn("Invalid Gerrit session token for user '" + username + "'");
      return null;
    }

    if (!session.isSignedIn()) {
      log.warn("Gerrit session " + session.getSessionId() + " is not signed-in");
      return null;
    }

    if (!session.getCurrentUser().getUserName().equals(username)) {
      log.warn("Gerrit session " + session.getSessionId()
          + " is not assigned to user " + username);
      return null;
    }

    return new GerritToGitBlitUserModel(username, projectControl);
  }

  public UserModel authenticateBasicAuth(String username, String password) {
    if (Strings.isNullOrEmpty(username) || password == null
        || password.length() <= 0) {
      log.warn("Authentication failed: no username or password specified");
      return null;
    }

    AuthRequest who = AuthRequest.forUser(username);
    who.setPassword(new String(password));

    try {
      AuthResult authResp = accountManager.authenticate(who);
      webSession.get().login(authResp, false);
    } catch (AccountException e) {
      log.warn("Authentication failed for '" + username + "'", e);
      return null;
    }

    return new GerritToGitBlitUserModel(username, projectControl);
  }

  @Override
  public UserModel getUserModel(String username) {

    return new GerritToGitBlitUserModel(username, projectControl);
  }

  @Override
  public boolean supportsCookies() {
    return false;
  }

  @Override
  public void setup(IStoredSettings settings) {
  }

  @Override
  public boolean supportsCredentialChanges() {
    return false;
  }

  @Override
  public boolean supportsDisplayNameChanges() {
    return false;
  }

  @Override
  public boolean supportsEmailAddressChanges() {
    return false;
  }

  @Override
  public boolean supportsTeamMembershipChanges() {
    return false;
  }

  @Override
  public String getCookie(UserModel model) {
    return model.cookie;
  }

  @Override
  public UserModel authenticate(char[] cookie) {
    return null;
  }

  @Override
  public void logout(UserModel user) {
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
    return null;
  }

  @Override
  public List<UserModel> getAllUsers() {
    return null;
  }

  @Override
  public List<String> getAllTeamNames() {
    return null;
  }

  @Override
  public List<TeamModel> getAllTeams() {
    return null;
  }

  @Override
  public List<String> getTeamnamesForRepositoryRole(String role) {
    return null;
  }

  @Override
  public boolean setTeamnamesForRepositoryRole(String role,
      List<String> teamnames) {
    return false;
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
    return null;
  }

  @Override
  public boolean setUsernamesForRepositoryRole(String role,
      List<String> usernames) {
    return false;
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
}
