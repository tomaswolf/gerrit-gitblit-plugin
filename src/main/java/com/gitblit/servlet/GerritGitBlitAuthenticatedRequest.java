package com.gitblit.servlet;

import javax.servlet.http.HttpServletRequest;

import com.gitblit.models.UserModel;
import com.gitblit.servlet.AuthenticationFilter.AuthenticatedRequest;

/**
 * This class exists only to make a method that inexplicably has package visibility public.
 */
public class GerritGitBlitAuthenticatedRequest extends AuthenticatedRequest {

	public GerritGitBlitAuthenticatedRequest(HttpServletRequest req) {
		super(req);
	}

	@Override
	public void setUser(UserModel user) {
		// Make this operation accessible publicly,in particular to the WrappedSyndicationFilter
		super.setUser(user);
	}

}
