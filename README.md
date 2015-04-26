# Gerrit-GitBlit plugin

This is a fork of [Luca Milanesio's](https://github.com/lucamilanesio) ["official" Gerrit-GitBlit plugin](https://gerrit.googlesource.com/plugins/gitblit/),
forked originally from [master revision 28d2c98](https://gerrit.googlesource.com/plugins/gitblit/+/28d2c9823618812acd21ce64f89c7e0ac47ff2a8).

It integrates [GitBlit](https://github.com/gitblit/gitblit) as a repository browser into [Gerrit](https://code.google.com/p/gerrit/) as a Gerrit plugin.

Pre-built jars (Java 7) are available as **[releases](https://github.com/tomaswolf/gerrit-gitblit-plugin/releases)**. Pick one with a version number
matching your Gerrit version. Version numbering for this plugin is the Gerrit API version it was built for, followed by the collapsed GitBlit version,
followed by the plugin version.So "v2.9.1.162.2" is version 2 of this plugin, integrating GitBlit 1.6.2 into Gerrit 2.9.1. Since the Gerrit API should
be [stable](https://gerrit-documentation.storage.googleapis.com/Documentation/2.10.1/dev-plugins.html#API) across minor version number increments, a
plugin built against 2.9.1 also works with Gerrit 2.9, and one built against 2.10.1 should also work with 2.10.

## Motivation

The basic reason for doing this was to adapt the official plugin to work with a modern Gerrit (v2.9 or newer) and a modern GitBlit (v1.6.2).

The official plugin is a bit dated by now. Luca described his integration in a [slideshow](http://www.slideshare.net/lucamilanesio/gitblit-plugin-for-gerrit-code-review).
Basically, this Gerrit-GitBlit plugin depends on a hacked version of Apache Wicket (classloading in UI), and on a hacked version
of Apache Rome (classloading in RSS feeder). Additionally, it only works with a specially hacked version of GitBlit 1.4.0. In
particular, Luca moved all the static resources in GitBlit into a "/static" subdirectory so that they'd be accessible by the
standard Gerrit plugin mechanism, which does handle this directory specially.

This works more or less, but has a number of problems:

* The official plugin doesn't produce branch graphs. That's [Gerrit bug 2942](https://code.google.com/p/gerrit/issues/detail?id=2942).
* It somehow doesn't serve the Flash copy-paste helper.
* GitBlit 1.4.0 produces diagrams and graphs using the Google charts API, making requests to Google.
* RSS feeds didn't work for me.
* The official plugin sets the base path for GitBlit to Gerrit's git directory. It should point somewhere else.
* Using a specially hacked GitBlit jar hosted at an apparently non-browseable Maven repository at GerritForge is a lock-in that
  I wanted to avoid. It means there's no reasonably easy path to get bug fixes in GitBlit since this fork of GitBlit was produced.
  I wanted to have a Gerrit-GitBlit plugin working with the latest standard official release of GitBlit from
  the standard official [GitBlit maven repo](http://gitblit.github.io/gitblit-maven/).

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
* I've given GitBlit its own base directory (at Gerrit's `$GERRIT_SITE/etc/gitblit`) to avoid that it creates subdirectories
  in the git repository directory that don't have anything to do with git repositories.
* I've disabled GitBlit's own plugin mechanism. Two layers of plugins is too confusing, might not work as expected anyway, and in all
  likelihood is not needed since GitBlit is used only as a repository viewer in this plugin.
* The dependency on GitBlit has been changed from Luca's special GitBlit version to the standard GitBlit 1.6.x distribution.
* The dependencies for Apache Wicket and Apache Rome have been changed to the standard distributions.
* The dependency on the Gerrit API has been changed from a snapshot version to the latest official release.
* Removed all the transitive dependencies from the `pom.xml`.
* The whole authentication/user model logic had to be refactored due to GitBlit changes.
* GitBlit 1.6.x still uses the [dagger injection framework](http://square.github.io/dagger/) (though it doesn't make much use of it).
  To make that work in GitBlit 1.6.x with the Guice-configured Gerrit plugin, it was necessary to add a fully-blown bridge
  module to make dagger use the Guice injector. (Which also meant I had to install m2e-apt in my Eclipse and enable it, and
  add the dagger dependencies.)

  > Off-topic: GitBlit 1.7.0 will not use dagger anymore but Guice.

* I've introduced a new servlet to serve those static resources from wherever they are in the original standard GitBlit jar.
  This also serves clippy.swf correctly now.

Additional modifications were due to changes in GitBlit:

* GitBlit 1.6.x uses the [flotr2](https://github.com/HumbleSoftware/Flotr2) library to generate charts and graphs. It doesn't make
  requests to Google anymore. But to get that to work in the plugin, some more URL rewriting was necessary to make the flotr2 static
  resources be served from within the plugin jar.
* GitBlit 1.6.x switched to the [pegdown](https://github.com/sirthias/pegdown) [markdown](https://en.wikipedia.org/wiki/Markdown)
  parser. That caused me some headache because Gerrit versions smaller than 2.11 include a version of pegdown that is too old for
  GitBlit.

# Installation

Download the [latest release](https://github.com/tomaswolf/gerrit-gitblit-plugin/releases) for your Gerrit version and install
the downloaded jar file as `gitblit.jar` in Gerrit.

If [remote plugin administration](https://gerrit-documentation.storage.googleapis.com/Documentation/2.9.1/config-gerrit.html#plugins)
is enabled in Gerrit, this can be done for instance by doing (assuming you downloaded `gitblit-plugin.VERSION.jar`)
```
ssh YOUR_GERRIT_URL gerrit plugin install - -n gitblit < gitblit-plugin.VERSION.jar
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

See the built-in [documentation](https://github.com/tomaswolf/gerrit-gitblit-plugin/blob/master/Documentation/index.md), which since
version v2.11.162.2 is after installation also available as _\<Your_Gerrit_URL>_/plugins/gitblit/Documentation/ or through the
"GitBlit&rarr;Documentation" menu item.

# Caveats and To-dos

* I have not gotten around to try out Lucene indexing of Gerrit repositories in GitBlit.
  
  *TODO 1*: I should give it a try and see if there any problems, and if so, if they can be worked around relatively easily in a self-contained
  way in the plugin itself. Even nicer: could GitBlit be made (easily!) to use Gerrit's Lucene index?
  
* I run this plugin (v2.9.1.162.2) in a firewalled private network, and it seems to me that the authentication stuff is good enough. I do _not_ know
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

I use Eclipse for development with m2e and m2e-apt installed. Make sure m2e-apt is enabled for the project.

To build, run the maven target "package", for instance from the Eclipse IDE right-click the pom.xml file, select "Maven build..." from the
context menu, enter "package" as target and click "Run". This produces a file "gitblit-plugin-_VERSION_.jar" in the target directory.
Install that in Gerrit as `gitblit.jar`. It should end up in `$GERRIT_SITE/plugins/gitblit.jar`.

Since I do not know and do not use [buck](http://facebook.github.io/buck/), I have removed the two `BUCK` files. This is a pure maven project.
If you need the BUCK files for some reason, [check them out](https://gerrit.googlesource.com/plugins/gitblit/+/1c2f070def1d37b28bde5a8a9eee8e26b9a9560c)
from the official plugin and adapt them to match the `pom.xml`.

# Alternatives

Some time after I had released my first version of this plugin, Luca Milanesio had updated the [official plugin](https://gerrit.googlesource.com/plugins/gitblit/)
to work again with Gerrit release 2.11. Internally, it still uses a specially hacked Apache Wicket and Rome, and it's based on an as
yet unreleased GitBlit version (James' [development branch](https://github.com/gitblit/gitblit/tree/develop) that should one day become
GitBlit 1.7.0). You can find that "official plugin" on the [Gerrit CI server](https://ci.gerritforge.com/job/Plugin_gitblit_stable-2.11/).
I have never used it, so I have no idea how well it works.

The official versions of this plugin in the Gerrit repo for Gerrit versions smaller than 2.11 are all based on the old GitBlit 1.4.0 and all exhibit the
problems mentioned above.