/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.utils;

import static com.vladsch.flexmark.ext.wikilink.WikiLinkExtension.WIKI_LINK;
import static com.vladsch.flexmark.profiles.pegdown.Extensions.ALL;
import static com.vladsch.flexmark.profiles.pegdown.Extensions.ANCHORLINKS;
import static com.vladsch.flexmark.profiles.pegdown.Extensions.SMARTYPANTS;
import static com.vladsch.flexmark.profiles.pegdown.Extensions.HARDWRAPS;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.text.MessageFormat;

import org.apache.commons.io.IOUtils;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.wikilink.WikiImage;
import com.vladsch.flexmark.ext.wikilink.internal.WikiLinkLinkResolver;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentLinkResolverFactory;
import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.options.MutableDataHolder;

/**
 * Utility methods for transforming raw markdown text to html.
 *
 * @author James Moger
 *
 */
public class MarkdownUtils {

	/**
	 * Returns the html version of the plain source text.
	 *
	 * @param text
	 * @return html version of plain text
	 * @throws java.text.ParseException
	 */
	public static String transformPlainText(String text) {
		// url auto-linking
		text = text.replaceAll("((http|https)://[0-9A-Za-z-_=\\?\\.\\$#&/]*)", "<a href=\"$1\">$1</a>");
		String html = "<pre>" + text + "</pre>";
		return html;
	}


	/**
	 * Returns the html version of the markdown source text.
	 *
	 * @param markdown
	 * @return html version of markdown text
	 * @throws java.text.ParseException
	 */
	public static String transformMarkdown(String markdown) {
		return transformMarkdown(markdown, null);
	}

	public interface Linker {

		ResolvedLink link(ResolvedLink original, boolean image);

	}

	/**
	 * Returns the html version of the markdown source text.
	 *
	 * @param markdown
	 * @return html version of markdown text
	 * @throws java.text.ParseException
	 */
	public static String transformMarkdown(String markdown, Linker linkRenderer) {
		MutableDataHolder options;

		if (linkRenderer == null) {
			options = PegdownOptionsAdapter.flexmarkOptions(ALL & ~SMARTYPANTS & ~ANCHORLINKS & ~HARDWRAPS).toMutable();
		} else {
			options = PegdownOptionsAdapter.flexmarkOptions(ALL & ~SMARTYPANTS & ~ANCHORLINKS & ~HARDWRAPS, new CustomExtension(linkRenderer)).toMutable();
		}
		Node document = Parser.builder(options).build().parse(markdown);
		return HtmlRenderer.builder(options).build().render(document);
	}

	private static class CustomExtension implements HtmlRenderer.HtmlRendererExtension {

		private final Linker resolver;

		public CustomExtension(Linker resolver) {
			this.resolver = resolver;
		}

		@Override
		public void rendererOptions(final MutableDataHolder options) {
			// Nothing
		}

		@Override
		public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
			rendererBuilder.linkResolverFactory(new Factory());
		}

		private class Resolver extends WikiLinkLinkResolver {

			public Resolver(LinkResolverContext context) {
				super(context);
			}

			@Override
			public ResolvedLink resolveLink(Node node, LinkResolverContext context, ResolvedLink link) {
				if (link.getLinkType() == WIKI_LINK && link.getUrl().indexOf("://") < 0) {
					ResolvedLink result = resolver.link(link, node instanceof WikiImage);
					if (result != null) {
						return result;
					}
				}
				return super.resolveLink(node, context, link);
			}
		}

		private class Factory extends IndependentLinkResolverFactory {

			@Override
			public LinkResolver create(final LinkResolverContext context) {
				return new Resolver(context);
			}
		}
	}
	/**
	 * Returns the html version of the markdown source reader. The reader is
	 * closed regardless of success or failure.
	 *
	 * @param markdownReader
	 * @return html version of the markdown text
	 * @throws java.text.ParseException
	 */
	public static String transformMarkdown(Reader markdownReader) throws IOException {
		// Read raw markdown content and transform it to html
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(markdownReader, writer);
			String markdown = writer.toString();
			return transformMarkdown(markdown);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// IGNORE
			}
		}
	}


	/**
	 * Transforms GFM (Github Flavored Markdown) to html.
	 * Gitblit does not support the complete GFM specification.
	 *
	 * @param input
	 * @param repositoryName
	 * @return html
	 */
	public static String transformGFM(IStoredSettings settings, String input, String repositoryName) {
		String text = input;

		// strikethrough
		text = text.replaceAll("~~(.*)~~", "<s>$1</s>");
		text = text.replaceAll("\\{(?:-){2}(.*)(?:-){2}}", "<s>$1</s>");

		// underline
		text = text.replaceAll("\\{(?:\\+){2}(.*)(?:\\+){2}}", "<u>$1</u>");

		// strikethrough, replacement
		text = text.replaceAll("\\{~~(.*)~>(.*)~~}", "<s>$1</s><u>$2</u>");

		// highlight
		text = text.replaceAll("\\{==(.*)==}", "<span class='highlight'>$1</span>");

		String canonicalUrl = settings.getString(Keys.web.canonicalUrl, "https://localhost:8443");

		// emphasize and link mentions
		String mentionReplacement = String.format(" **[@$1](%1s/user/$1)**", canonicalUrl);
		text = text.replaceAll("\\s@([A-Za-z0-9-_]+)", mentionReplacement);

		// link ticket refs
		String ticketReplacement = MessageFormat.format("$1[#$2]({0}/tickets?r={1}&h=$2)$3", canonicalUrl, repositoryName);
		text = text.replaceAll("([\\s,]+)#(\\d+)([\\s,:\\.\\n])", ticketReplacement);

		// link commit shas
		int shaLen = settings.getInteger(Keys.web.shortCommitIdLength, 6);
		String commitPattern = MessageFormat.format("\\s([A-Fa-f0-9]'{'{0}'}')([A-Fa-f0-9]'{'{1}'}')", shaLen, 40 - shaLen);
		String commitReplacement = String.format(" [`$1`](%1$s/commit?r=%2$s&h=$1$2)", canonicalUrl, repositoryName);
		text = text.replaceAll(commitPattern, commitReplacement);

		String html = transformMarkdown(text);
		return html;
	}
}
