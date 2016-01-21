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
package com.googlesource.gerrit.plugins.gitblit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.AvatarGenerator;
import com.gitblit.GravatarGenerator;
import com.gitblit.IStoredSettings;
import com.gitblit.guice.IPublicKeyManagerProvider;
import com.gitblit.guice.WorkQueueProvider;
import com.gitblit.manager.FederationManager;
import com.gitblit.manager.FilestoreManager;
import com.gitblit.manager.GitblitManager;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.IFilestoreManager;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IServicesManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.manager.NotificationManager;
import com.gitblit.manager.ProjectManager;
import com.gitblit.manager.ServicesManager;
import com.gitblit.servlet.BranchGraphServlet;
import com.gitblit.servlet.DownloadZipServlet;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.servlet.LogoServlet;
import com.gitblit.servlet.PagesServlet;
import com.gitblit.servlet.PtServlet;
import com.gitblit.servlet.RawServlet;
import com.gitblit.tickets.ITicketService;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.gitblit.utils.JSoupXssFilter;
import com.gitblit.utils.WorkQueue;
import com.gitblit.utils.XssFilter;
import com.gitblit.wicket.GitBlitWebApp;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.internal.UniqueAnnotations;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlitContext;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlitRuntimeManager;
import com.googlesource.gerrit.plugins.gitblit.app.GerritGitBlitWebApp;
import com.googlesource.gerrit.plugins.gitblit.app.GitBlitSettings;
import com.googlesource.gerrit.plugins.gitblit.app.NullPluginManager;
import com.googlesource.gerrit.plugins.gitblit.app.ReallyNullTicketService;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritGitBlitAuthenticationManager;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritGitBlitRepositoryManager;
import com.googlesource.gerrit.plugins.gitblit.auth.GerritGitBlitUserManager;

public class GitBlitServletModule extends ServletModule {
	private static final Logger log = LoggerFactory.getLogger(GitBlitServletModule.class);

	@Inject
	public GitBlitServletModule(@PluginName final String name) {
		log.info("Create GitBlitModule with name '{}'", name);
	}

	@Override
	protected void configureServlets() {
		log.info("Configuring servlet and filters");
		// Plugin life-cycle listener
		bind(PluginActivator.class);
		bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create()).to(PluginActivator.class);

		// Changed things
		bind(IStoredSettings.class).to(GitBlitSettings.class);
		bind(IRuntimeManager.class).to(GerritGitBlitRuntimeManager.class);
		bind(IUserManager.class).to(GerritGitBlitUserManager.class);
		bind(IAuthenticationManager.class).to(GerritGitBlitAuthenticationManager.class);
		bind(IRepositoryManager.class).to(GerritGitBlitRepositoryManager.class);
		bind(GitblitContext.class).to(GerritGitBlitContext.class);
		bind(GitBlitWebApp.class).to(GerritGitBlitWebApp.class);
		bind(IPluginManager.class).to(NullPluginManager.class);
		bind(ITicketService.class).to(ReallyNullTicketService.class);

		// Gitblit bindings
		bind(XssFilter.class).to(JSoupXssFilter.class);
		bind(AvatarGenerator.class).to(GravatarGenerator.class);
		bind(WorkQueue.class).toProvider(WorkQueueProvider.class);

		bind(IGitblit.class).to(GitblitManager.class);

		// core managers
		bind(IPluginManager.class).to(NullPluginManager.class);
		bind(INotificationManager.class).to(NotificationManager.class);
		bind(IProjectManager.class).to(ProjectManager.class);
		bind(IFederationManager.class).to(FederationManager.class);
		bind(IFilestoreManager.class).to(FilestoreManager.class);
		bind(IPublicKeyManager.class).toProvider(IPublicKeyManagerProvider.class);

		// manager for long-running daemons and services
		bind(IServicesManager.class).to(ServicesManager.class);

		// Servlets -- note: FilestoreServlet is not configured
		serve('/' + WrappedPagesFilter.SERVLET_RELATIVE_PATH + '*').with(PagesServlet.class);
		serve('/' + WrappedRawFilter.SERVLET_RELATIVE_PATH + '*').with(RawServlet.class);
		serve('/' + WrappedSyndicationFilter.SERVLET_RELATIVE_PATH + '*').with(WrappedSyndicationServlet.class);
		serve("/zip/*").with(DownloadZipServlet.class);
		serve("/logo.png").with(LogoServlet.class);
		serve("/static/logo.png").with(LogoServlet.class);
		serve("/graph/*").with(BranchGraphServlet.class);
		serve("/static/*").with(StaticResourcesServlet.class);
		serve("/clippy.swf").with(StaticResourcesServlet.class);
		serve("/pt").with(PtServlet.class);

		// Filters
		filter("/*").through(GerritWicketFilter.class);
		filter('/' + WrappedPagesFilter.SERVLET_RELATIVE_PATH + '*').through(WrappedPagesFilter.class);
		filter('/' + WrappedRawFilter.SERVLET_RELATIVE_PATH + '*').through(WrappedRawFilter.class);
		filter('/' + WrappedSyndicationFilter.SERVLET_RELATIVE_PATH + '*').through(WrappedSyndicationFilter.class);
	}
}
