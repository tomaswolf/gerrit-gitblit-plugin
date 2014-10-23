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
package com.googlesource.gerrit.plugins.gitblit;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PluginActivator implements LifecycleListener {
	private static final Logger log = LoggerFactory.getLogger(PluginActivator.class);

	private final String pluginName;

	private final GerritWicketFilter filter;

	@Inject
	public PluginActivator(@PluginName String pluginName, GerritWicketFilter filter) {
		this.pluginName = pluginName;
		this.filter = filter;
		// Just some string that is unique per plugin instance. This is used ultimately in
		// GerritGitBlitWebApp.newRequestCycleProcessor to expunge stale Java objects attached to
		// HTTP session by Wicket. (They become "stale" in a plugin reload, because they will have
		// been loaded by the class loader of the unloaded plugin instance, but then are accessed
		// by the new plugin instance, which has a new classloader. The result is funny
		// ClassCastExceptions telling you that "GitBlitWebSession cannot be cast to Session" even
		// though, if you look at the source code, GitBlitWebSession very clearly is derived from
		// Session. I think I understand why the GitBlit author, James Moger, has said several times
		// he finds Wicket's stateful model a pain.
		filter.setPluginInstanceKey(Long.toHexString(new SecureRandom().nextLong()) + Long.toHexString(System.nanoTime())
				+ Long.toHexString(System.currentTimeMillis()));
	}

	@Override
	public void start() {
		// Just so that we can see in the log whether this activator is invoked.
		log.info("Starting plugin {}", pluginName);
	}

	@Override
	public void stop() {
		log.info("Stopping plugin {}", pluginName);
		// Wicket internally keeps a number of session-related things around. To support clean Gerrit plugin reloading,
		// we must be sure that this data survives in proper form. The main problem here is that the HTTP Session is
		// kept across the plugin reload, and Wicket stores stuff keyed by sessionId. The call below ultimately will
		// serialize Wicket's per-session page state to disk, where it will be found again when the new plugin instance
		// starts up. Since the data is serialized and deserialized, this works even if the Class instances change.
		filter.destroy();
		// Note that Wicket also stores some unserialized Java objects directly in the HTPP Session. That causes
		// ClassCastExceptions if the newly started plugin instance then tries to retrieve them, because they have
		// been loaded by a different classloader (the one of the previous plugin instance). There's two problems
		// with that:
		// - we don't have access to any session here, and I have not found any way to enumerate all sessions known
		//   to Wicket.
		// - even if we somehow could access these sessions, I've not found any clean way to tell Wicket that it
		//   should serialize its Java objects attached to that session, and de-serialize them again when the new
		//   plugin instance has started and comes across such a HTTP session again.
		// Therefore, we have to deal either in the GerritWicketFilter or in the request cycle processor with possibly
		// stale orphaned Java objects attached to the HTTP session. See GerritGitBlitWebApp.newRequestCycleProcessor.
		log.info("Filter destroyed {}", pluginName);
	}

}
