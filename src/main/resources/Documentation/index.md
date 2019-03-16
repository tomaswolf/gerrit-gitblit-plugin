# GitBlit plugin

This plugin integrates [GitBlit](https://github.com/gitblit/gitblit) **BUILDREPLACE{GitBlit-Version}** as a repository browser into [Gerrit](https://code.google.com/p/gerrit/),
with full SSO through Gerrit.

* License: [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* [Home page](https://github.com/tomaswolf/gerrit-gitblit-plugin)
* Installed plugin version: <em id='gerrit-gitblit-current-version'>BUILDREPLACE{pom.version}</em> &mdash; <a id='gerrit-gitblit-version-check' style='display:none;' href='#'>Check for updates</a>

For a list of contributors, see at [GitHub](https://github.com/tomaswolf/gerrit-gitblit-plugin/graphs/contributors).

This is a privately maintained fork of the official Gerrit-Gitblit plugin. Please report any issues with this plugin at the [GitHub issue tracker](https://github.com/tomaswolf/gerrit-gitblit-plugin/issues).

> For the official plugin, see the repository at [Google Code](https://gerrit.googlesource.com/plugins/gitblit/+/master).

# Configuration

The plugin in configured in Gerrit's global configuration file `gerrit.config`.
As of v2.16.171.0, this plugin does _not_ use the `[gitweb]` section anymore.
Configuration is done in the `[plugin "@PLUGIN@"]` section.

This plugin adds a "GitBlit" top menu to Gerrit. Since v2.11.162.2 of this plugin, the link
texts for all sub-menu items can be configured to your taste in a `[plugin "@PLUGIN@"]` section in your `gerrit.config`. If the section is not present,
or some values in that section are not defined, the plugin uses built-in default texts. The default configuration would correspond to

	[plugin "@PLUGIN@"]
	        repositories = Repositories
	        activity = Activity
	        documentation = Documentation
	        search =
	        linkname = Gitblit

The first four are sub-menu items of the "GitBlit" top menu, the last one is the link text shown on links to Gitblit for individual files
or in the project list.

The "search" sub-menu item is by default not set and will thus not be shown. Setting it makes only sense if you enable GitBlit indexing on some of
your projects. See the [GitBlit documentation](http://gitblit.com/setup_lucene.html) for more information on that.

## GitBlit configuration

The plugin includes a minimal default configuration to make GitBlit act only as a repository viewer. You can augment that with further
customizations in [a normal `gitblit.properties`](http://gitblit.com/properties.html) file located in Gerrit's `$GERRIT_SITE/etc` directory.
The built-in configuration, which ensures that GitBlit is configured as a viewer only, always takes precedence. Also, the `git.repositoriesFolder`
property is always set to Gerrit's git directory at `$GERRIT_SITE/git`.

To see the built-in configuration, access it at [`gitblit.properties`](@URL@plugins/@PLUGIN@/static/gitblit.properties).

By default, the built-in configuration does allow anonymous browsing, subject to the repository and ref-level access restrictions defined in Gerrit.
If you want to lock the GitBlit plugin to allow only logged-in users to browse, set in `$GERRIT_SITE/etc/gitblit.properties` the key
`web.authenticateViewPages = true`. This is the only key of the built-in configuration that you _can_ override.

GitBlit's ticket service, fan-out service, and its plugin mechanism are disabled in this plugin, as is ssh access through GitBlit since Gerrit
already provides that. Also disabled is Gitblit's LFS implementation.

The GitBlit `${baseFolder}` is the plugin's data directory provided by Gerrit at `$GERRIT_SITE/data/@PLUGIN@/`.

> Up to and including v2.11.162.1 of this plugin, GitBlit's `${baseFolder}` was at `$GERRIT_SITE/etc/@PLUGIN@/`. If you upgraded from that or an earlier
> version and do have data in `$GERRIT_SITE/etc/@PLUGIN@/`, move it over to the new place. The directory `$GERRIT_SITE/etc/@PLUGIN@/` can then be removed.

### GitBlit settings specific to this plugin

<dl>
	<dt><code>web.maxCommitDiffContext</code> = [1 .. 10]</dt>
	<dd>
		<p>
			<em>Since 2.12.162.2.</em> In a GitBlit <em>commit diff</em>, diffs of all changed files in a commit are shown on one page. This setting
			defines how many context lines shall be shown shown at most in such diffs. The default is 10, but can be reduced further through this property
			in your <code>gitblit.properties</code> file.
		</p>
		<p>
			If the Gerrit preference setting for the number of context lines in diffs is lower than this GitBlit setting, the Gerrit setting is taken.
		</p>
		<p>
			For single-file diffs, the plugin respects the Gerrit preference setting of the currently logged-in user for the number of context lines
			in a diff. For non-logged-in users, the GitBlit default of 3 context lines applies. (The Gerrit default is 10.)
		</p>
	</dd>
</dl>


# Issue tracking

Report bugs or make feature requests at the [GitHub issue tracker](https://github.com/tomaswolf/gerrit-gitblit-plugin/issues).

## Known issues

* For logged-in users, their preference page ("my profile") is accessible but non-functional. Per-user GitBlit preferences cannot be saved. There are no
  plans to fix this; GitBlit uses a plain file-based mechanism to store such user preferences anyway, which won't scale well for large user bases.

* I have never tried Lucene indexing of repositories through GitBlit. It's functionality I have never needed, and I don't plan to ever do something about
  it in case it doesn't work.

<hr style="color: #C0C0C0; background-color: #C0C0C0; border-color: #C0C0C0; height: 2px;" />
<div style="float:right;">
<a href="https://github.com/tomaswolf/gerrit-gitblit-plugin" target="_blank">GitBlit plugin BUILDREPLACE{pom.version}</a>
</div>
<script type="text/javascript" src="version_check.js"></script>
