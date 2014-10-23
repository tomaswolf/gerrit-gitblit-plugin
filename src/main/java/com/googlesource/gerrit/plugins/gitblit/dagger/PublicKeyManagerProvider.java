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
package com.googlesource.gerrit.plugins.gitblit.dagger;

import com.gitblit.Keys;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.transport.ssh.FileKeyManager;
import com.gitblit.transport.ssh.IPublicKeyManager;
import com.gitblit.transport.ssh.MemoryKeyManager;
import com.gitblit.transport.ssh.NullKeyManager;
import com.gitblit.utils.StringUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class PublicKeyManagerProvider implements Provider<IPublicKeyManager> {

	private final IRuntimeManager runtimeManager;

	@Inject
	public PublicKeyManagerProvider(final IRuntimeManager manager) {
		this.runtimeManager = manager;
	}

	@Override
	public IPublicKeyManager get() {
		String clazz = runtimeManager.getSettings().getString(Keys.git.sshKeysManager, FileKeyManager.class.getName());
		if (StringUtils.isEmpty(clazz)) {
			clazz = FileKeyManager.class.getName();
		}
		if (FileKeyManager.class.getName().equals(clazz)) {
			return new FileKeyManager(runtimeManager);
		} else if (NullKeyManager.class.getName().equals(clazz)) {
			return new NullKeyManager();
		} else if (MemoryKeyManager.class.getName().equals(clazz)) {
			return new MemoryKeyManager();
		} else {
			try {
				Class<?> mgrClass = Class.forName(clazz);
				return (IPublicKeyManager) mgrClass.newInstance();
			} catch (Exception e) {
			}
			return new NullKeyManager();
		}
	}

}
