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
package com.googlesource.gerrit.plugins.gitblit.dagger;

import com.gitblit.IStoredSettings;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.manager.IFederationManager;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.INotificationManager;
import com.gitblit.manager.IPluginManager;
import com.gitblit.manager.IProjectManager;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.gitblit.utils.XssFilter;
import com.gitblit.wicket.GitBlitWebApp;
import com.google.inject.Inject;
import com.google.inject.Injector;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger-Guice bridge module for all our overridden GitBlit managers.
 * 
 * Needed because GitblitContext uses dagger injections. We provide this module instead of the standard module. Note that this essentially replaces dagger
 * injection by Guice injection.
 */
@com.google.inject.Singleton
@Module(library = true, injects = {
// @format:off
	IStoredSettings.class,
	XssFilter.class,
	// Overridden core managers
	IRuntimeManager.class,
	IUserManager.class,
	IAuthenticationManager.class,
	IRepositoryManager.class,
	// The monolithic manager
	IGitblit.class,
	// The Gitblit Wicket app
	GitBlitWebApp.class,
	// Unchanged but wrapped managers
	IPluginManager.class,
	INotificationManager.class,
	IPublicKeyManager.class,
	IProjectManager.class,
	IFederationManager.class
	// @format:on
})
public final class GerritDaggerModule {

	private final Injector injector;

	@Inject
	public GerritDaggerModule(final Injector injector) {
		this.injector = injector;
	}

	@Provides
	@javax.inject.Singleton
	IStoredSettings provideSettings() {
		return injector.getInstance(IStoredSettings.class);
	}

	@Provides
	@javax.inject.Singleton
	XssFilter provideXssFilter() {
		return injector.getInstance(XssFilter.class);
	}

	@Provides
	@javax.inject.Singleton
	IRuntimeManager provideRuntimeManager() {
		return injector.getInstance(IRuntimeManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IUserManager provideUserManager() {
		return injector.getInstance(IUserManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IAuthenticationManager provideAuthenticationManager() {
		return injector.getInstance(IAuthenticationManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IRepositoryManager provideRepositoryManager() {
		return injector.getInstance(IRepositoryManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IGitblit provideGitblit() {
		return injector.getInstance(IGitblit.class);
	}

	@Provides
	@javax.inject.Singleton
	GitBlitWebApp provideWebApplication() {
		return injector.getInstance(GitBlitWebApp.class);
	}

	// Wrapped managers. We do need to wrap them because we need some of them to instantiate some of the
	// above. Therefore, we need Guice bindings for them, and thus we'd better make sure that dagger does
	// use the same instances of all these singletons.

	@Provides
	@javax.inject.Singleton
	IPluginManager providePluginManager() {
		return injector.getInstance(IPluginManager.class);
	}

	@Provides
	@javax.inject.Singleton
	INotificationManager provideNotificationManager() {
		return injector.getInstance(INotificationManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IPublicKeyManager providePublicKeyManager() {
		return injector.getInstance(IPublicKeyManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IProjectManager provideProjectManager() {
		return injector.getInstance(IProjectManager.class);
	}

	@Provides
	@javax.inject.Singleton
	IFederationManager provideFederationManager() {
		return injector.getInstance(IFederationManager.class);
	}

}
