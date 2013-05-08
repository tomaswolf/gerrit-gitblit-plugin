package com.googlesource.gerrit.plugins.gitblit;

import java.util.Arrays;
import java.util.List;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;

@Listen
public class GitBlitTopMenu implements TopMenu {
  private final MenuEntry fullMenuEntries;
  private final MenuEntry restrictedMenuEntries;
  private final Provider<CurrentUser> userProvider;

  @Inject
  public GitBlitTopMenu(final @PluginName String pluginName,
      final Provider<CurrentUser> userProvider) {
    this.userProvider = userProvider;

    String gitBlitBaseUrl = "/plugins/" + pluginName + "/";
    this.restrictedMenuEntries =
        menu("Gitblit", item("Repositories", gitBlitBaseUrl + "repositories/"));
    this.fullMenuEntries =
        menu("GitBlit", item("Repositories", gitBlitBaseUrl + "repositories/"),
            item("Activity", gitBlitBaseUrl + "activity/"),
            item("Search", gitBlitBaseUrl + "lucene/"));
  }

  private MenuEntry menu(String name, MenuItem... items) {
    return new MenuEntry(name, Arrays.asList(items));
  }

  private MenuItem item(String name, String url) {
    return new MenuItem(name, url, "");
  }

  @Override
  public List<MenuEntry> getEntries() {
    return Arrays.asList(userProvider.get() instanceof AnonymousUser
        ? restrictedMenuEntries : fullMenuEntries);
  }
}
