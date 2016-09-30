# Gerrit-GitBlit plugin

This is a fork of the [official Gerrit-GitBlit plugin](https://gerrit.googlesource.com/plugins/gitblit/),
forked originally from [master revision 28d2c98](https://gerrit.googlesource.com/plugins/gitblit/+/28d2c9823618812acd21ce64f89c7e0ac47ff2a8).

It integrates [GitBlit](https://github.com/gitblit/gitblit) as a repository browser into [Gerrit](https://code.google.com/p/gerrit/) as a Gerrit plugin.

Pre-built jars (Java 7) are available as **[releases](https://github.com/tomaswolf/gerrit-gitblit-plugin/releases)**. Pick one with a version number
matching your Gerrit version. Version numbering for this plugin is the Gerrit API version it was built for, followed by the collapsed GitBlit version,
followed by the plugin version.So "v2.9.1.162.2" is version 2 of this plugin, integrating GitBlit 1.6.2 into Gerrit 2.9.1.

> If you're running Gerrit 2.11 or newer, you might want to check whether the official plugin fulfills your needs. (To find a pre-built official plugin,
> go to the [Gerrit CI server](https://gerrit-ci.gerritforge.com/), find the "Plugin-gitblit" job matching your Gerrit version, click the link, and download
> the jar from "Last Successful Artifacts".)

## Motivation

The basic reason for doing this was to adapt the official plugin to work with a modern Gerrit (v2.9 or newer) and a modern GitBlit (v1.6.2 and later v1.7.1).
This was done at a time when Gerrit 2.9 and 2.10 were the current Gerrit releases, and the official plugin just didn't work well.

The official plugin was a bit dated by then. Luca described his integration in a [slideshow](http://www.slideshare.net/lucamilanesio/gitblit-plugin-for-gerrit-code-review).
Basically, this Gerrit-GitBlit plugin depended on a hacked version of Apache Wicket (classloading in UI), and on a hacked version
of Apache Rome (classloading in RSS feeder). Additionally, it only worked with a specially hacked version of GitBlit 1.4.0. In
particular, Luca had moved all the static resources in GitBlit into a "/static" subdirectory so that they'd be accessible by the
standard Gerrit plugin mechanism, which does handle this directory specially.

This worked more or less, but still has one problem left:
* The official plugin set the base path for GitBlit to Gerrit's git directory. It should point somewhere else.

You might [wonder](https://groups.google.com/forum/#!topic/repo-discuss/yi6IG_Xgekc) whether the whole approach of including
GitBlit as a Gerrit plugin makes sense at all. The basic problem this plugin tries to overcome is the lack of any decent repository
browser in Gerrit. While you can make it use GitBlit, gitweb, gitiles or any other external browser, I don't readily see how one
would propagate all the view and action permissions defined in Gerrit to such an external browser, be it GitBlit or something else.
Luca's integration approach has the benefit that these rights can rather easily be propagated to GitBlit, so if a user cannot see
or modify a branch or repository in Gerrit, he also won't be able to see it through this GitBlit plugin.

Still, Gerrit is a git server, and shoving a fully blown down-configured other git server like GitBlit into Gerrit to get a
repository browser with a nice UI smells like overkill. I share James Moger's [puzzlement](https://groups.google.com/d/msg/repo-discuss/yi6IG_Xgekc/uzLUoMInGD0J)
over this, but since GitBlit has a very nice UI indeed and works well, it's an easy way to get what I want. True, I'd much
prefer having a stripped-down GitBlit component that included only the parts relevant to browsing and viewing repositories, but no
such component exists, GitBlit is not modular enough to quickly extract those parts (or I don't know how), and unless GitBlit
itself offers such a component, creating one would just create another specially hacked GitBlit fork.

## What's changed?

A lot.

Well, maybe not that much. But GitBlit has changed quite bit since 1.4.0; its internal structure is different, and thus the
Guice injection setup must be much more complete. Whereas the original plugin was able to get this to work mostly with just wrapping
some servlets and setting them up through Guice, one has to do quite a bit more with GitBlit 1.6.x.

* I've fixed the "missing branch graphs": [Gerrit bug 2942](https://code.google.com/p/gerrit/issues/detail?id=2942)
* I've made the RSS feed work.
* I've given GitBlit its own base directory to avoid that it creates subdirectories in the git repository directory that don't have
  anything to do with git repositories.
* I've disabled GitBlit's own plugin mechanism. Two layers of plugins is too confusing, might not work as expected anyway, and in all
  likelihood is not needed since GitBlit is used only as a repository viewer in this plugin.
* The dependency on GitBlit has been changed from Luca's special GitBlit version to the standard GitBlit 1.6.x distribution.
* The dependencies for Apache Wicket and Apache Rome have been changed to the standard distributions.
* The dependency on the Gerrit API has been changed from a snapshot version to the latest official release.
* Removed all the transitive dependencies from the `pom.xml`.
* The whole authentication/user model logic had to be refactored due to GitBlit changes.
* GitBlit 1.6.x still used the [dagger injection framework](http://square.github.io/dagger/). Since GitBlit 1.7.0 , Guice is used.
* I've introduced a new servlet to serve those static resources from wherever they are in the original standard GitBlit jar.
  This also serves clippy.swf correctly now.

Additional modifications were due to changes in GitBlit:

* GitBlit 1.6.x/1.7.x uses the [flotr2](https://github.com/HumbleSoftware/Flotr2) library to generate charts and graphs. It doesn't make
  requests to Google anymore. But to get that to work in the plugin, some more URL rewriting was necessary to make the flotr2 static
  resources be served from within the plugin jar.
* GitBlit 1.6.x switched to the [pegdown](https://github.com/sirthias/pegdown) [markdown](https://en.wikipedia.org/wiki/Markdown)
  parser. That caused me some headache because Gerrit versions smaller than 2.11 include a version of pegdown that is too old for
  GitBlit.

The [official version of GitBlit](https://gerrit.googlesource.com/plugins/gitblit/+/refs/heads/master) has now included most of those improvements and run on a plain GitBlit 1.7.1 version.

# Installation

Download the [latest release](https://github.com/tomaswolf/gerrit-gitblit-plugin/releases) for your Gerrit version and install
the downloaded jar file as `gitblit.jar` in Gerrit.

If [remote plugin administration](https://gerrit-documentation.storage.googleapis.com/Documentation/2.9.1/config-gerrit.html#plugins)
is enabled in Gerrit, this can be done for instance by doing (assuming you downloaded `gitblit-plugin.VERSION.jar`)
```
ssh YOUR_GERRIT_URL gerrit plugin install - -n gitblit.jar < gitblit-plugin.VERSION.jar
ssh YOUR_GERRIT_URL gerrit plugin reload gitblit
``` 

This is a fairly large plugin, adding many classes to the JVM Gerrit runs in. Depending on what kind of JVM you're using, I
cannot exclude the possibility that adding GitBlit to Gerrit might lead to "java.lang.OutOfMemoryError: PermGen space". If you
ever observe this, increase the "PermGen" memory for Gerrit. For the Oracle HotSpot JVM < 8.0, you'd do that by adding the option 
`container.javaOptions = -XX:MaxPermSize=320m` (or whatever size you deem appropriate) to `gerrit.config`. Possibly you
then also might want to increase Gerrit's maximum heap size a bit (that's `container.heapLimit`).
See the [Gerrit documentation](https://gerrit-documentation.storage.googleapis.com/Documentation/2.9.1/config-gerrit.html#container).

As of HotSpot 8.0, this "PermGen space" issue should not occur, and the "-XX:MaxPermSize" option has even been removed from HotSpot.
See the [Java 8 compatibility guide](http://www.oracle.com/technetwork/java/javase/8-compatibility-guide-2156366.html). The reason
is that HotSpot 8.0 stores the class metadata no longer in the Java heap but in native memory, like IBM's J9 VM does.

# Configuration

See the built-in [documentation](https://github.com/tomaswolf/gerrit-gitblit-plugin/blob/master/docu/index.md),
which since version v2.11.162.2 is after installation also available as _\<Your_Gerrit_URL>_/plugins/gitblit/Documentation/ or through the
"GitBlit&rarr;Documentation" menu item.

# Caveats and To-dos

* I have not gotten around to try out Lucene indexing of Gerrit repositories in GitBlit. Since this is a feature I don't use, I have no plans
  to do anything about this in case it doesn't work.
  
* I run this plugin in a firewalled private network, and it seems to me that the authentication stuff is good enough. I do _not_ know
  whether it would be good enough for running this on a public network, or whether I goofed somewhere big time. I'm no web security
  expert and cannot make any guarantees. I strongly suspect that the RSS feed does not honour ref-level visibility restrictions; it only
  honours repository-level visibility.

* I do _not_ know how well GitBlit scales. It does not seem to be clusterable: according to its author, James Moger,
  "[Gitblit is heavily filesystem based and does not support clustering.](https://groups.google.com/forum/#!topic/gitblit/Puc_3o-zTd0)"
  Additionally, he gives "[Small workgroups that require centralized repositories.](http://gitblit.com/faq.html#H15)" as the target
  audience. I run this plugin for such a group, and it appears to work fine.

* In any case, see **points 7 and 8 of the [`LICENSE`](https://github.com/tomaswolf/gerrit-gitblit-plugin/blob/master/LICENSE)** 
  (no guarantees, no warranty, no liability).

# Building

I use Eclipse for development with m2e and m2e-apt installed. Make sure m2e-apt is enabled for the project. (It's not really needed to
build the plugin with Gitblit 1.7.x, since that uses Guice. It's only needed to build versions of the plugin based on Gitblit 1.6.x.)

To build, run the maven target "package", for instance from the Eclipse IDE right-click the pom.xml file, select "Maven build..." from the
context menu, enter "package" as target and click "Run". This produces a file "gitblit-plugin-_VERSION_.jar" in the target directory.
Install that in Gerrit as `gitblit.jar`. It should end up in `$GERRIT_SITE/plugins/gitblit.jar`.

Since I do not know and do not use [buck](http://facebook.github.io/buck/), I have removed the two `BUCK` files. This is a pure maven project.
If you need the BUCK files for some reason, [check them out](https://gerrit.googlesource.com/plugins/gitblit/+/1c2f070def1d37b28bde5a8a9eee8e26b9a9560c)
from the official plugin and adapt them to match the `pom.xml`.

# Alternatives

Some time after I had released my first version of this plugin, Luca Milanesio had updated the [official plugin](https://gerrit.googlesource.com/plugins/gitblit/) thanks to my and the community's contributions,
to work again with Gerrit release 2.11 and 2.12. Internally it's based on plain GitBlit version 1.7.x. You can find that official plugin on the
[Gerrit CI server](https://gerrit-ci.gerritforge.com/view/Plugins-stable-2.11/job/plugin-gitblit-stable-2.11/). I have never used it, so I have no idea how well it works.

As previously mentioned, in May 2015, some of my changes here were contributed, reviewed and merged back into the official plugin, but there are still functional differences, mainly
related to plugin reloading, raw file serving, handling of non-logged-in users, and how `gitblit.properties` is loaded and what default
settings are provided by the plugin.
