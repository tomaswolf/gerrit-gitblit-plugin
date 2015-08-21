package com.googlesource.gerrit.plugins.gitblit;

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

}
