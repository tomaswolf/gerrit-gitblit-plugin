package com.googlesource.gerrit.plugins.gitblit.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Cleaner;

import com.gitblit.utils.JSoupXssFilter;

public class FixedUrlXSSFilter extends JSoupXssFilter {

	@Override
	protected String clean(String input, Cleaner cleaner) {
		// We know that this is only ever called through none() from the GitblitParamUrlCodingStrategy. This exists only to strip HTML from URL
		// request parameters. The cleaner is one that strips all HTML tags. I'm not entirely sure this is a good thing to do. What if I have a
		// file named "<body>.txt" in my git repository?? GitBlit uses filenames quite often as URL parameters, so it won't be able to deal
		// with such files! Care has to be taken when such file names are written to HTML again, but on input?
		//
		// Anyway, the problem here is that the final html() call at the end will encode some characters as HTML character entities, which then
		// breaks a lot of things later on. We must undo that (after all, we already know that all HTML has been stripped). I just hope the rest
		// of GitBlit knows what it does when it writes pathes to HTML and properly encodes them then.
		Document unsafe = Jsoup.parse(input);
		Document safe = cleaner.clean(unsafe);
		// Let's restrict JSoup to only encode lt, gt, amp, apos, and quot. Less clean-up work to do later on.
		safe.outputSettings().escapeMode(EscapeMode.xhtml);
		String sanitized = safe.body().html();
		// And now undo that encoding. We *know* that by now, there's no HTML in the string, just isolated occurrences of these "critical"
		// characters.
		return sanitized.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&apos;", "'").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
	}
}
