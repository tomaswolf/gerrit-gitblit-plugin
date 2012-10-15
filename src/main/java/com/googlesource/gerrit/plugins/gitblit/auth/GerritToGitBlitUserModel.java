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

import java.util.HashSet;
import java.util.Set;

import com.gitblit.Constants.AccessPermission;
import com.gitblit.Constants.AccessRestrictionType;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import com.gitblit.utils.StringUtils;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.RefControl;

public class GerritToGitBlitUserModel extends UserModel {

  private static final long serialVersionUID = 1L;

  // field names are reflectively mapped in EditUser page
  public String username;
  public String password;
  public String cookie;
  public String displayName;
  public String emailAddress;
  public boolean canAdmin;
  public boolean excludeFromFederation;
  public final Set<String> repositories = new HashSet<String>();
  public final Set<TeamModel> teams = new HashSet<TeamModel>();

  private transient final ProjectControl.Factory projectControlFactory;

  // non-persisted fields
  public boolean isAuthenticated;

  public GerritToGitBlitUserModel(String username) {
    this(username, null);
  }

  public GerritToGitBlitUserModel(String username,
      final ProjectControl.Factory projectControlFactory) {
    super(username);
    this.username = username;
    this.isAuthenticated = true;
    this.projectControlFactory = projectControlFactory;
  }

  @Deprecated
  public boolean canAccessRepository(String repositoryName) {
    boolean result = false;

    try {
      ProjectControl control =
          projectControlFactory.validateFor(new NameKey(repositoryName));
      result = control != null;
    } catch (NoSuchProjectException e) {
      result = false;
    }

    return result;
  }

  @Override
  protected boolean canAccess(RepositoryModel repository,
      AccessRestrictionType ifRestriction, AccessPermission requirePermission) {
    boolean result = false;

    try {
      ProjectControl control =
          projectControlFactory.validateFor(new NameKey(
              getRepositoryName(repository.name)));

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
    } catch (NoSuchProjectException e) {
      result = false;
    }

    return result;
  }

  public String getRepositoryName(String name) {
    if (name.endsWith(".git")) {
      name = name.substring(0, name.length() - 4);
    }
    return name;
  }

  @Override
  public boolean hasRepositoryPermission(String name) {
    boolean result = false;

    try {
      name = getRepositoryName(name);
      ProjectControl control =
          projectControlFactory.validateFor(new NameKey(name));
      result = control != null && control.isVisible();
    } catch (NoSuchProjectException e) {
      result = false;
    }

    return result;
  }

  @Override
  public boolean hasBranchPermission(String repoName, String branchRef) {
    boolean result = false;

    try {
      repoName = getRepositoryName(repoName);

      ProjectControl control =
          projectControlFactory.validateFor(new NameKey(repoName));
      if (control != null && control.isVisible()) {
        RefControl branchCtrl = control.controlForRef(branchRef);
        result = branchCtrl != null && branchCtrl.isVisible();
      }

    } catch (NoSuchProjectException e) {
      result = false;
    }

    return result;
  }

  public boolean hasTeamAccess(String repositoryName) {
    for (TeamModel team : teams) {
      if (team.hasRepository(repositoryName)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasRepository(String name) {
    return repositories.contains(name.toLowerCase());
  }

  public void addRepository(String name) {
    repositories.add(name.toLowerCase());
  }

  public void removeRepository(String name) {
    repositories.remove(name.toLowerCase());
  }

  public boolean isTeamMember(String teamname) {
    for (TeamModel team : teams) {
      if (team.name.equalsIgnoreCase(teamname)) {
        return true;
      }
    }
    return false;
  }

  public TeamModel getTeam(String teamname) {
    if (teams == null) {
      return null;
    }
    for (TeamModel team : teams) {
      if (team.name.equalsIgnoreCase(teamname)) {
        return team;
      }
    }
    return null;
  }

  @Override
  public String getName() {
    return username;
  }

  public String getDisplayName() {
    if (StringUtils.isEmpty(displayName)) {
      return username;
    }
    return displayName;
  }

  @Override
  public String toString() {
    return username;
  }

  @Override
  public int compareTo(UserModel o) {
    return username.compareTo(o.username);
  }
}
