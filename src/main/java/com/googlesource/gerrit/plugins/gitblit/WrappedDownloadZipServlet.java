package com.googlesource.gerrit.plugins.gitblit;

import com.gitblit.DownloadZipServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WrappedDownloadZipServlet extends DownloadZipServlet {
  private static final long serialVersionUID = 1348780775920545752L;

  @Inject
  public WrappedDownloadZipServlet() {
    super();
  }
}
