// Copyright (C) 2019 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.gitblit;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginCanonicalWebUrl;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.common.WebLinkInfo;
import com.google.gerrit.extensions.webui.BranchWebLink;
import com.google.gerrit.extensions.webui.FileHistoryWebLink;
import com.google.gerrit.extensions.webui.FileWebLink;
import com.google.gerrit.extensions.webui.ParentWebLink;
import com.google.gerrit.extensions.webui.PatchSetWebLink;
import com.google.gerrit.extensions.webui.ProjectWebLink;
import com.google.gerrit.extensions.webui.TagWebLink;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GitBlitWebUrls implements BranchWebLink, FileWebLink, PatchSetWebLink, ProjectWebLink, FileHistoryWebLink, ParentWebLink, TagWebLink {

	private static final Logger log = LoggerFactory.getLogger(GitBlitWebUrls.class);

	private final String name;
	private final String baseUrl;

	@Inject
	public GitBlitWebUrls(@PluginName String pluginName, @PluginCanonicalWebUrl String pluginUrl, @GerritServerConfig Config config) {
		String baseGerritUrl = "/plugins/" + pluginName + '/';
		try {
			if (pluginUrl != null) {
				URL u = new URL(pluginUrl);
				baseGerritUrl = u.getPath();
				if (!baseGerritUrl.endsWith("/")) {
					baseGerritUrl += '/';
				}
			}
		} catch (MalformedURLException e) {
			log.error("Canonical web URL is malformed: " + pluginUrl, e);
		}
		baseUrl = baseGerritUrl;
		String linkName = config.getString("plugin", pluginName, "linkname");
		if (linkName == null || linkName.isEmpty()) {
			linkName = config.getString("gitweb", null, "linkname");
		}
		if (linkName == null || linkName.isEmpty()) {
			linkName = "Gitblit";
		}
		name = linkName;
	}

	@Override
	public WebLinkInfo getProjectWeblink(String projectName) {
		return new WebLinkInfo(name, null, baseUrl + String.format("summary/?r=%s", projectName), Target.BLANK);
	}

	@Override
	public WebLinkInfo getPatchSetWebLink(String projectName, String commit) {
		return new WebLinkInfo(name, null, baseUrl + String.format("commit/?r=%s&h=%s", projectName, commit), Target.BLANK);
	}

	@Override
	public WebLinkInfo getParentWebLink(String projectName, String commit) {
		return getPatchSetWebLink(projectName, commit);
	}

	@Override
	public WebLinkInfo getFileWebLink(String projectName, String revision, String fileName) {
		return new WebLinkInfo(name, null, baseUrl + String.format("blob/?r=%s&h=%s&f=%s", projectName, revision, fileName), Target.BLANK);
	}

	@Override
	public WebLinkInfo getBranchWebLink(String projectName, String branchName) {
		return new WebLinkInfo(name, null, baseUrl + String.format("log/?r=%s&h=%s", projectName, branchName), Target.BLANK);
	}

	@Override
	public WebLinkInfo getTagWebLink(String projectName, String tagName) {
		return new WebLinkInfo(name, null, baseUrl + String.format("log/?r=%s&h=%s", projectName, tagName), Target.BLANK);
	}

	@Override
	public WebLinkInfo getFileHistoryWebLink(String projectName, String revision, String fileName) {
		return new WebLinkInfo(name, null, baseUrl + String.format("history/?f=%s&r=%s&h=%s", fileName, projectName, revision), Target.BLANK);
	}
}
