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

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

import com.gitblit.GitBlit;
import com.gitblit.models.UserModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritToGitBlitUserService;

@Singleton
public class GerritGitBlit extends GitBlit {

  @Inject
  public GerritGitBlit(GerritToGitBlitUserService userService) {
    super(userService);
  }

  public UserModel authenticate(HttpServletRequest request) {
    String user = (String) request.getAttribute("gerrit-username");
    String token = (String) request.getAttribute("gerrit-token");
    if (token == null) {
      return null;
    }

    return GitBlit.self().authenticate(user,
        (GerritToGitBlitUserService.SESSIONAUTH + token).toCharArray());
  }

  @Override
  public InputStream getResourceAsStream(String file)
      throws ResourceStreamNotFoundException {
    String resourceName = "/static/" + file;
    InputStream is = getClass().getResourceAsStream(resourceName);
    if (is == null) {
      throw new ResourceStreamNotFoundException("Cannot access resource "
          + resourceName + " using class-loader " + getClass().getClassLoader());
    }

    return is;
  }
}
