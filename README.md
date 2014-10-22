# Gerrit-GitBlit plugin

This is a fork of [Luca Milanesio's](https://github.com/lucamilanesio) ["official" Gerrit-GitBlit plugin](https://gerrit.googlesource.com/plugins/gitblit/),
forked originally from [master revision 28d2c98](https://gerrit.googlesource.com/plugins/gitblit/+/28d2c9823618812acd21ce64f89c7e0ac47ff2a8).

It integrates [GitBlit](https://github.com/gitblit/gitblit) as a repository browser into [Gerrit](https://code.google.com/p/gerrit/) as a Gerrit plugin.

Pre-built jars (Java 7) are available as **[releases](https://github.com/tomaswolf/gerrit-gitblit-plugin/releases)**.

## Motivation

The basic reason for doing this was to adapt the official plugin to work with a modern Gerrit (v2.9.1) and a modern GitBlit (v1.6.x).

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

Well, maybe not that much. But GitBlit has changed quite bit since 1.4.0; it's internal structure is different, and thus the
Guice injection setup must be much more complete. Whereas the original plugin was able to get this to work mostly with just wrapping
some servlets and setting them up through Guice, one has to do quite a bit more with GitBlit 1.6.x.

* I've fixed the "missing branch graphs": [Gerrit bug 2942](https://code.google.com/p/gerrit/issues/detail?id=2942)
* I've made the RSS feed work.
* I've given GitBlit its own base directory (at Gerrit's `$GERRIT_SITE/etc/gitblit`) to avoid that it creates subdirectories
  in the git repository directory that don't have anything to do with git repositories (specifically, a tickets and a plugins
  directory -- off-topic thought: what happens if you add GitBlit plugins to this Gerrit plugin?).
* The dependency on GitBlit has been changed from Luca's special GitBlit version to the standard GitBlit 1.6.0 distribution.
* The dependencies for Apache Wicket and Apache Rome have been changed to the standard distributions.
* The dependency on the Gerrit API has been changed from 2.9-SNAPSHOT to the official 2.9.1 release.
* Removed all the transitive dependencies from the `pom.xml`.
* The whole authentication/user model logic had to be refactored due to GitBlit changes.
* GitBlit 1.6.x still uses the [dagger injection framework](http://square.github.io/dagger/) (though it doesn't make much use of it).
  To make that work in GitBlit 1.6.x with the Guice-configured Gerrit plugin, it was necessary to add a fully-blown bridge
  module to make dagger use the Guice injector. (Which also meant I had to install m2e-apt in my Eclipse and enable it, and
  add the dagger dependencies.)

  > Off-topic: Maybe GitBlit would be much better off using Guice in the first place. Currently, it uses dagger
  only in a rather restricted way, apparently because dagger's use of the standard `javax.inject` annotations conflicts with
  typical web CDI containers that also use those, pick them up, and then want to do their own injections, which results in an
  unholy mess all over the place. Guice doesn't use the `javax.inject` annotations.

* I've introduced a new servlet to serve those static resources from wherever they are in the original standard GitBlit jar.
  This also serves clippy.swf correctly now.

Additional modifications were due to changes in GitBlit:

* GitBlit 1.6.x uses the [flotr2](https://github.com/HumbleSoftware/Flotr2) library to generate charts and graphs. It doesn't make
  requests to Google anymore. But to get that to work in the plugin, some more URL rewriting was necessary to make the flotr2 static
  resources be served from within the plugin jar.
* GitBlit 1.6.x switched to the [pegdown](https://github.com/sirthias/pegdown) [markdown](https://en.wikipedia.org/wiki/Markdown)
  parser.
  
The last point proved to be rather nasty. Gerrit itself uses and includes pegdown and exposes it in the Gerrit API. However,
Gerrit has pegdown 1.2.1, while GitBlit uses pegdown 1.4.2. Gerrit plugins inherit a standard "parent-first" Java class loader
(with the parent being the Gerrit API), which means that GitBlit will always use the pegdown 1.2.1 from the Gerrit API, not
the pegdown 1.4.2 it includes itself. That causes exceptions at runtime because GitBlit makes use of features that don't exist
in pegdown 1.2.1 yet.

I was able to work around this by relocating GitBlit's pegdown in the shaded jar that is built for the plugin, only to discover
that I also needed to relocate the [parboiled](https://github.com/sirthias/parboiled) library it's built on. At that point I
ran into a [bug in parboiled](https://github.com/sirthias/parboiled/issues/80).

> Off-topic: I'm rather puzzled by the approach taken by pegdown/parboiled. It creates a parser at runtime by dynamically creating bytecode.
  Maybe I'm missing something, but that smells fishy to me. Makes me think of the good ol' times(??) when self-modifying code was considered clever.
  Why can't one generate this parser at compile time in source form, avoiding the runtime overhead and facilitating debugging?

To work around that, I had to include the sources of [parboiled-java](https://github.com/sirthias/parboiled/tree/master/parboiled-java/src/main/java/org/parboiled)
and [fix that bug](https://github.com/sirthias/parboiled/pull/82/files) myself. If and when Gerrit ever upgrades to
pegdown 1.4.2, it should be possible to remove this work-around (and the parboiled-java sources from this project).

# Configuration

There's two different configurations: one for Gerrit so it knows how to generate links that will be processed by the plugin, and
an optional GitBlit configuration for the plugin itself.

## Gerrit configuration

In Gerrit's `gerrit.config`, define the `[gitweb]` section as follows (assuming the plugin was installed as `gitblit.jar`):

```
[gitweb]
        type = custom
        url = plugins/
        linkname = browse
        project = gitblit/summary/?r=${project}
        revision = gitblit/commit/?r=${project}&h=${commit}
        branch = gitblit/log/?r=${project}&h=${branch}
        filehistory = gitblit/history/?f=${file}&r=${project}&h=${branch}
        file = gitblit/blob/?r=${project}&h=${commit}&f=${file}
        roottree = gitblit/tree/?r=${project}&h=${commit}
```

This is normally done automatically if you add the pugin and run through `java -jar gerrit.war init -d site_path`, but you can also
add this manually to Gerrit's config file. The `linkname` can be adapted to your taste.

## GitBlit configuration

The plugin includes a minimal default configuration to make GitBlit act only as a repository viewer. You can augment that with further
customizations in a normal [`gitblit.properties`](http://gitblit.com/properties.html) file located in Gerrit's `$GERRIT_SITE/etc` directory.
The built-in configuration, which ensures that GitBlit is configured as a viewer only, always takes precedence. Also, the `git.repositoriesFolder`
property is always set to Gerrit's git directory at `$GERRIT_SITE/git`.

The GitBlit `${basePath}` is in Gerrit's `$GERRIT_SITE/etc` directory at `$GERRIT_SITE/etc/gitblit/`.

Note that the loading order of GitBlit configurations is different from Luca's original plugin. The original plugin read _either_ the user-supplied
file _or_ the built-in configuration, which meant if you wanted to customize GitBlit, you had to include the contents of the built-in config in your
own file. That is no longer necessary with _this_ plugin.

To see the built-in configuration, access it at _\<Your_Gerrit_URL>_/plugins/gitblit/static/gitblit.properties.

By default, the built-in configuration does allow anonymous browsing, subject to the repository and ref-level vaccess restrictions defined in Gerrit.
If you want to lock the GitBlit plugin to allow only logged-in users to browse, set in `$GERRIT_SITE/etc/gitblit.properties` the key
`web.authenticateViewPages = true`. This is the only key of the built-in configuration that you _can_ override. 

# Caveats and To-dos

* I have not gotten around to try out Lucene indexing of Gerrit repositories in GitBlit.
  
  *TODO 1*: I should give it a try and see if there any problems, and if so, if they can be worked around relatively easily in a self-contained
  way in the plugin itself. Even nicer: could GitBlit be made (easily!) to use Gerrit's Lucene index?
  
* I run this plugin in a firewalled private network, and it seems to me that the authentication stuff is good enough. I do _not_ know
  whether it would be good enough for running this on a public network, or whether I goofed somewhere big time. I'm no web security
  expert and cannot make any guarantees. I strongly suspect that the RSS feed does not honour ref-level visibility restrictions; it only
  honours repository-level visibility.
  
* GitBlit 1.6.0 has a bug that will make the "raw" links fail for repositories in nested directories under `$GERRIT_SITE/git`. It works
  for repositories located directly in that directory. This bug has been fixed in GitBlit 1.6.1.

* In any case, see **points 7 and 8 of the [`LICENSE`](https://github.com/tomaswolf/gerrit-gitblit-plugin/blob/master/LICENSE)** 
  (no guarantees, no warranty, no liability).

# Building

I use Eclipse for development (currently Kepler) with m2e and m2e-apt installed. Make sure m2e-apt is enabled for the project.

To build, run the maven target "package", for instance from the Eclipse IDE right-click the pom.xml file, select "Maven build..." from the
context menu, enter "package" as target and click "Run". This produces a file "gitblit-plugin-_VERSION_.jar" in the target directory.
Install that in Gerrit as `gitblit.jar`. It should end up in `$GERRIT_SITE/plugins/gitblit.jar`.

Since I do not know and do not use [buck](http://facebook.github.io/buck/), I have removed the two `BUCK` files. This is a pure maven project.
If you need the BUCK files for some reason, [check them out](https://gerrit.googlesource.com/plugins/gitblit/+/1c2f070def1d37b28bde5a8a9eee8e26b9a9560c)
from the official plugin and adapt them to match the `pom.xml`.