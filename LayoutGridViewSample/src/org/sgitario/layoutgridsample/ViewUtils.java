package org.sgitario.layoutgridsample;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.squareup.picasso.Picasso;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewUtils {
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	
	public static void setImageUrl(ImageView view, String url) {
		if (url != null) {
			view.setVisibility(ImageView.VISIBLE);
			Picasso.with(view.getContext()).load(url).into(view);
		} else {
			view.setVisibility(ImageView.GONE);
		}
	}

	public static void setText(TextView view, String text) {
		if (text != null) {
			view.setVisibility(TextView.VISIBLE);

			view.setText(text);
		} else {
			view.setVisibility(TextView.GONE);
		}
	}
	
	public static void setDateFrom(TextView view, Date when) {
		if (when != null) {
			Date now = new Date();
			
			long timeDiffInMinutes = (now.getTime() - when.getTime()) / 60000;
			if (timeDiffInMinutes < 1) {
				ViewUtils.setText(view, "now");
			} else if (timeDiffInMinutes < 60) {
				ViewUtils.setText(view, timeDiffInMinutes + " min ago");
			} else if (timeDiffInMinutes < (60 * 24)) {
				ViewUtils.setText(view, timeDiffInMinutes / 60 + " hour ago");
			} else {
				ViewUtils.setText(view, DATE_FORMAT.format(when));
			}
			
			view.setVisibility(TextView.VISIBLE);
		} else {
			view.setVisibility(TextView.GONE);
		}
	}

	public static void linkify(TextView view) {
		TransformFilter filter = new TransformFilter() {
			public final String transformUrl(final Matcher match, String url) {
				return match.group();
			}
		};

		Pattern mentionPattern = Pattern.compile("@([A-Za-z0-9_-]+)");
		String mentionScheme = "http://www.twitter.com/";
		Linkify.addLinks(view, mentionPattern, mentionScheme, null, filter);

		Pattern hashtagPattern = Pattern.compile("#([A-Za-z0-9_-]+)");
		String hashtagScheme = "http://www.twitter.com/search/";
		Linkify.addLinks(view, hashtagPattern, hashtagScheme, null, filter);

		Pattern urlPattern = Patterns.WEB_URL;
		Linkify.addLinks(view, urlPattern, null, null, filter);
		
		stripUnderlines(view);
	}

	private static void stripUnderlines(TextView textView) {
		if (textView.getText() instanceof Spannable) {
			Spannable s = (Spannable) textView.getText();
			URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
			for (URLSpan span : spans) {
				int start = s.getSpanStart(span);
				int end = s.getSpanEnd(span);
				s.removeSpan(span);
				span = new URLSpanNoUnderline(span.getURL());
				s.setSpan(span, start, end, 0);
			}
			
			textView.setText(s);
		}
	}

	private static class URLSpanNoUnderline extends URLSpan {
		public URLSpanNoUnderline(String url) {
			super(url);
		}

		@Override
		public void updateDrawState(TextPaint ds) {
			super.updateDrawState(ds);
			ds.setUnderlineText(false);
		}
	}
}
