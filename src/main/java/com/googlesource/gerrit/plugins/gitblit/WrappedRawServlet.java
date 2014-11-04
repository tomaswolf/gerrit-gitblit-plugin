package com.googlesource.gerrit.plugins.gitblit;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gitblit.servlet.RawServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WrappedRawServlet extends RawServlet {
	private static final long serialVersionUID = -3566561701268611822L;

	@Inject
	public WrappedRawServlet() {
		super();
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.service(new FixedGuiceHttpServletRequest(request), response);
	}

}
