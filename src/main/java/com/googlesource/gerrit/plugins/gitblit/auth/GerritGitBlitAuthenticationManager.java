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
package com.googlesource.gerrit.plugins.gitblit.auth;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import com.gitblit.transport.ssh.SshKey;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.GitBlitWebSession;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.AuthResult;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GerritGitBlitAuthenticationManager implements IAuthenticationManager {

	private static final Logger log = LoggerFactory.getLogger(GerritGitBlitAuthenticationManager.class);

	private final AccountManager gerritAccountManager;
	private final Provider<WebSession> gerritSession;
	private final GerritGitBlitUserManager userManager;
	private final IStoredSettings settings;

	private final String pluginName;

	@Inject
	public GerritGitBlitAuthenticationManager(final AccountManager gerritAccountManager, final Provider<WebSession> gerritSession,
			final GerritGitBlitUserManager userManager, final IStoredSettings settings, @PluginName String pluginName) {
		this.gerritAccountManager = gerritAccountManager;
		this.gerritSession = gerritSession;
		this.userManager = userManager;
		this.settings = settings;
		this.pluginName = pluginName;
	}

	@Override
	public IAuthenticationManager start() {
		return this;
	}

	@Override
	public IAuthenticationManager stop() {
		return this;
	}

	@Override
	public UserModel authenticate(HttpServletRequest httpRequest) {
		// Added by the GerritAuthenticationFilter.
		String username = (String) httpRequest.getAttribute("gerrit-username");
		String token = (String) httpRequest.getAttribute("gerrit-token");
		String password = (String) httpRequest.getAttribute("gerrit-password");

		if (!Strings.isNullOrEmpty(token)) {
			return authenticateFromSession(username, token);
		} else if (!Strings.isNullOrEmpty(password)) {
			return authenticateViaGerrit(username, password);
		} else if (GerritGitBlitUserModel.ANONYMOUS_USER.equals(username)) {
			return userManager.getUserModel(username);
		}
		return null;
	}

	@Override
	public UserModel authenticate(String username, SshKey key) {
		return null;
	}

	@Override
	public UserModel authenticate(HttpServletRequest httpRequest, boolean requiresCertificate) {
		return authenticate(httpRequest);
	}

	private UserModel authenticateFromSession(String username, String token) {
		WebSession session = gerritSession.get();

		if (session.getSessionId() == null || !session.getSessionId().equals(token)) {
			log.warn("Invalid Gerrit session token for user '{}'", username);
			return null;
		}

		if (!session.isSignedIn()) {
			log.warn("Gerrit session {} is not signed-in", session.getSessionId());
			return null;
		}

		if (!session.getCurrentUser().getUserName().equals(username)) {
			log.warn("Gerrit session {} is not assigned to user {}", session.getSessionId(), username);
			return null;
		}

		return loggedIn(userManager.getUserModel(username), token, null);
	}

	@Override
	public UserModel authenticate(String username, char[] password) {
		return authenticateViaGerrit(username, new String(password));
	}

	private UserModel authenticateViaGerrit(String username, String password) {
		if (Strings.isNullOrEmpty(username)) {
			log.warn("Authentication failed: no username");
			return null;
		}
		if (Strings.isNullOrEmpty(password)) {
			log.warn("Authentication failed: no password for user '{}'", username);
			return null;
		}

		AuthRequest who = AuthRequest.forUser(username);
		who.setPassword(password);

		try {
			AuthResult authResp = gerritAccountManager.authenticate(who);
			gerritSession.get().login(authResp, false);
			log.info("Logged in {}", username);
			return loggedIn(userManager.getUserModel(username), password, authResp);
		} catch (AccountException e) {
			log.warn("Authentication failed for user '" + username + '\'', e);
			return null;
		}
	}

	@Override
	public String getCookie(HttpServletRequest request) {
		if (settings.getBoolean(Keys.web.allowCookieAuthentication, true)) {
			Cookie[] cookies = request.getCookies();
			if (cookies != null && cookies.length > 0) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(Constants.NAME)) {
						String value = cookie.getValue();
						return value;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void setCookie(HttpServletResponse response, UserModel user) {
		// XXX next version of GitBlit has a three-param setCookie (also gets the request)
		if (settings.getBoolean(Keys.web.allowCookieAuthentication, true)) {
			GitBlitWebSession session = GitBlitWebSession.get();
			boolean standardLogin = session.authenticationType.isStandard();

			if (standardLogin) {
				Cookie userCookie;
				if (user == null) {
					// clear cookie for logout
					userCookie = new Cookie(Constants.NAME, "");
				} else {
					// set cookie for login
					String cookie = userManager.getCookie(user);
					if (Strings.isNullOrEmpty(cookie)) {
						// create empty cookie
						userCookie = new Cookie(Constants.NAME, "");
					} else {
						// create real cookie
						userCookie = new Cookie(Constants.NAME, cookie);
						// expire the cookie in 7 days
						userCookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(7));
					}
				}
				userCookie.setPath("/plugins/" + pluginName);
				response.addCookie(userCookie);
			}
		}
	}

	@Override
	public void logout(HttpServletResponse response, UserModel user) {
		gerritSession.get().logout();
		setCookie(response, null);
	}

	@Override
	public boolean supportsCredentialChanges(UserModel user) {
		return false;
	}

	@Override
	public boolean supportsDisplayNameChanges(UserModel user) {
		return false;
	}

	@Override
	public boolean supportsEmailAddressChanges(UserModel user) {
		return false;
	}

	@Override
	public boolean supportsTeamMembershipChanges(UserModel user) {
		return false;
	}

	@Override
	public boolean supportsTeamMembershipChanges(TeamModel team) {
		return false;
	}

	private UserModel loggedIn(UserModel user, String credentials, AuthResult authentication) {
		if (authentication != null && !gerritSession.get().getCurrentUser().isIdentifiedUser()) {
			log.warn("Setting account after log-in for " + user.getName());
			// We just logged in via Gerrit. However, if somehow somewhere some code called getCurrentUser() on that WebSession object before,
			// the "current user object" remains stuck on the previous value. Happens for instance in WrappedSyndicationFilter after the 401
			// challenge. Frankly said, I don't know if that is a bug in Gerrit, or what's up. Methinks CacheBasedWebSession.login() should
			// (re-)set its private field "user" to null, so that the next call to getCurrentUser() re-computes the user object. We can get
			// around this by forcing the account id to the account we just authenticated. In this request, this user won't be able to do
			// Gerrit administration, but we're inside GitBlit anyway, so there's no need for this anyway.
			gerritSession.get().setUserAccountId(authentication.getAccountId());
		}
		user.cookie = StringUtils.getSHA1(user.getName() + credentials); // Code from GitBlit
		return user;
	}
}
