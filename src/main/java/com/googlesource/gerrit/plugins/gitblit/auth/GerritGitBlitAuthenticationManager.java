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

import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.Constants.AuthenticationType;
import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import com.gitblit.transport.ssh.SshKey;
import com.gitblit.utils.StringUtils;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginCanonicalWebUrl;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.AuthResult;
import com.google.gerrit.server.config.AuthConfig;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;

public class GerritGitBlitAuthenticationManager implements IAuthenticationManager {

	private static final Logger log = LoggerFactory.getLogger(GerritGitBlitAuthenticationManager.class);

	private final AccountManager gerritAccountManager;
	private final DynamicItem<WebSession> gerritSession;
	private final GerritGitBlitUserManager userManager;
	private final IStoredSettings settings;
	private final String gerritUrl;
	private final String externalLogoutUrl;

	/**
	 * Path part of the canonical plugin URL.
	 */
	private final String hostRelativePluginPath;

	@Inject
	public GerritGitBlitAuthenticationManager(final AccountManager gerritAccountManager, final DynamicItem<WebSession> gerritSession,
			final GerritGitBlitUserManager userManager, final IStoredSettings settings, @PluginName String pluginName,
			@PluginCanonicalWebUrl String pluginUrl, @CanonicalWebUrl String canonicalGerritUrl, AuthConfig authConfig) {
		this.gerritAccountManager = gerritAccountManager;
		this.gerritSession = gerritSession;
		this.userManager = userManager;
		this.settings = settings;
		this.gerritUrl = canonicalGerritUrl;
		this.externalLogoutUrl = authConfig.getLogoutURL();
		this.hostRelativePluginPath = extractPluginPath(pluginUrl, pluginName);
	}

	/**
	 * Determines the path part of the plugin Url. Should work OK even if Gerrit isn't at the root path, for instance at //somehost:someport/gerrit/.
	 * 
	 * @param pluginUrl
	 *            as determined by Gerrit's plugin infrastructure
	 * @param pluginName
	 *            name of this plugin
	 * @return the absolute path to the plugin on this server.
	 */
	private String extractPluginPath(String pluginUrl, String pluginName) {
		// Let's be defensive
		int idx = -1;
		if (pluginUrl != null) {
			idx = pluginUrl.indexOf("//");
			idx = pluginUrl.indexOf('/', idx < 0 ? 0 : idx + 2);
		}
		if (idx < 0) { // Huh?
			return "/plugins/" + pluginName; // A reasonable default; works only correctly if Gerrit is on the root path
		}
		// We do know that in any case it ends with /plugins/ + pluginName...
		return pluginUrl.substring(idx);
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
	public UserModel authenticate(final HttpServletRequest httpRequest) {
		// Added by the GerritAuthenticationFilter.
		String username = (String) httpRequest.getAttribute("gerrit-username");
		String token = (String) httpRequest.getAttribute("gerrit-token");
		String password = (String) httpRequest.getAttribute("gerrit-password");

		if (!Strings.isNullOrEmpty(token)) {
			return authenticateFromSession(httpRequest, username, token);
		} else if (!Strings.isNullOrEmpty(password)) {
			return authenticateViaGerrit(httpRequest, username, password);
		} else if (GerritGitBlitUserModel.ANONYMOUS_USER.equals(username)) {
			// XXX Do we really still need this branch? We "inherited" that from the old official plugin...
			return userManager.getUserModel(username);
		}
		return null;
	}

	@Override
	public UserModel authenticate(final String username, final SshKey key) {
		return null;
	}

	@Override
	public UserModel authenticate(final HttpServletRequest httpRequest, final boolean requiresCertificate) {
		return authenticate(httpRequest);
	}

	private UserModel authenticateFromSession(final HttpServletRequest httpRequest, String username, String token) {
		WebSession session = gerritSession.get();

		if (session.getSessionId() == null || !session.getSessionId().equals(token)) {
			log.warn("Invalid Gerrit session token for user '{}'", username);
			return null;
		}

		if (!session.isSignedIn()) {
			log.warn("Gerrit session {} is not signed-in", session.getSessionId());
			// XXX: maybe better return the anonymous user?
			// return userManager.getUserModel((String) null);
			return null;
		}

		String userName = session.getUser().getUserName();
		// Gerrit users not necessarily have a username. Google OAuth returns users without user names.
		UserModel user;
		if (Strings.isNullOrEmpty(userName)) {
			user = userManager.getUnnamedGerritUser();
		} else {
			if (!userName.equals(username)) {
				log.warn("Gerrit session {} is not assigned to user {}", session.getSessionId(), username);
				return null;
			}
			user = userManager.getUserModel(userName);
		}
		return loggedIn(httpRequest, user, token, null);
	}

	@Override
	public UserModel authenticate(String username, char[] password) {
		return authenticateViaGerrit(null, username, new String(password));
	}

	private UserModel authenticateViaGerrit(HttpServletRequest httpRequest, String username, String password) {
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
			return loggedIn(httpRequest, userManager.getUserModel(username), password, authResp);
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

	private boolean isStandardLogin(HttpServletRequest request) {
		if (request == null) {
			log.warn("(Internal) Deprecated cookie method called.");
			return false;
		}
		HttpSession session = request.getSession();
		AuthenticationType authenticationType = (AuthenticationType) session.getAttribute(Constants.AUTHENTICATION_TYPE);
		return authenticationType != null && authenticationType.isStandard();
	}

	@Override
	@Deprecated
	public void setCookie(HttpServletResponse response, UserModel user) {
		setCookie(null, response, user);
	}

	@Override
	public void setCookie(HttpServletRequest request, HttpServletResponse response, UserModel user) {
		if (settings.getBoolean(Keys.web.allowCookieAuthentication, true) && isStandardLogin(request)) {
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
			userCookie.setPath(hostRelativePluginPath);
			response.addCookie(userCookie);
		}
	}

	@Override
	@Deprecated
	public void logout(HttpServletResponse response, UserModel user) {
		logout(null, response, user);
	}

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, UserModel user) {
		gerritSession.get().logout();
		setCookie(request, response, null);
	}

	/**
	 * Logs out the user in GitBlit. Returns a URL to redirect to for Gerrit logout.
	 * 
	 * @return a URL to redirect to, or {@code null} if none.
	 */
	public String logoutAndRedirect(HttpServletRequest request, HttpServletResponse response, UserModel user) {
		if (!Strings.isNullOrEmpty(gerritUrl)) {
			setCookie(request, response, null);
			// This redirect invokes the normal Gerrit logout process, regardless of what authentication mechanism is configured,
			// so logout should work properly also for OAuth, OpenID, and also respect auth.logoutUrl.
			return gerritUrl + (gerritUrl.endsWith("/") ? "" : "/") + "logout";
		}
		log.warn("gerrit.config should define gerrit.canonicalWebUrl");
		// Try to log out ourselves. We have no access to the OAuth/OpenId sessions, so we can't do anything for those.
		// See {@link com.google.gerrit.httpd.HttpLogoutServlet}.
		logout(request, response, user);
		return externalLogoutUrl;
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

	private UserModel loggedIn(HttpServletRequest request, UserModel user, String credentials, AuthResult authentication) {
		if (authentication != null && !gerritSession.get().getUser().isIdentifiedUser()) {
			log.warn("Setting account after log-in for " + user.getName());
			// We just logged in via Gerrit. However, if somehow somewhere some code called getCurrentUser() on that WebSession object before,
			// the "current user object" remains stuck on the previous value. Happens for instance in WrappedSyndicationFilter after the 401
			// challenge. Frankly said, I don't know if that is a bug in Gerrit, or what's up. Methinks CacheBasedWebSession.login() should
			// (re-)set its private field "user" to null, so that the next call to getCurrentUser() re-computes the user object. We can get
			// around this by forcing the account id to the account we just authenticated. In this request, this user won't be able to do
			// Gerrit administration, but we're inside GitBlit anyway, so there's no need for this anyway.
			gerritSession.get().setUserAccountId(authentication.getAccountId());
		}
		if (request != null) {
			request.getSession().setAttribute(Constants.AUTHENTICATION_TYPE, AuthenticationType.CREDENTIALS);
		}
		user.cookie = StringUtils.getSHA1(user.getName() + credentials); // Code from GitBlit
		return user;
	}
}
