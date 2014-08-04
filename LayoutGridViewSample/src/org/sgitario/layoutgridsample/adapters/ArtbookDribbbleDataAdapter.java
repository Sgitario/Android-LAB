package org.sgitario.layoutgridsample.adapters;

import java.util.List;

import org.sgitario.layoutgridsample.R;
import org.sgitario.layoutgridsample.ViewUtils;
import org.sgitario.layoutgridsample.model.Feed;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

/**
 * Data adapter to update the feeds content and the fragment views.
 * 
 * @author jhilario
 */
public class ArtbookDribbbleDataAdapter extends ArrayAdapter<Feed> {
	
	public static final  String TAG = ArtbookDribbbleDataAdapter.class.toString();
	
	static class ViewHolder {
		ImageView pic;
	}
	
	private final Activity context;

	/**
	 * Initializes a new instance of the DribbbleDataAdapter class.
	 * @param context
	 */
	public ArtbookDribbbleDataAdapter(Activity context) {
		super(context, R.layout.pic_view);
		this.context = context;
	}
	
	/**
	 * Update the feeds by adding the specified feeds to the current data content and update the container.
	 * @param feeds
	 */
	public void update(List<Feed> feeds) {
		if (feeds != null) {
			for (Feed item : feeds) {
				this.add(item);
			}
			
			notifyDataSetChanged();
		}
	}

	/**
	 * get item identifier.
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Resolve the view for the current item view.
	 */
	@Override
	public View getView(int position, View convertView,
			ViewGroup parent) {
		
		ViewHolder holder = null;
		
		// If null, then the holder is new.
		if (convertView == null) {
			// inflate the layout
			convertView = LayoutInflater.from(context).inflate(
					R.layout.pic_view, null, false);
			
			// well set up the ViewHolder
			holder = new ViewHolder();
			holder.pic = (ImageView) convertView.findViewById(R.id.pic);
			
			// store the holder with the view.
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// Retrieve feed to show.
		final Feed s = this.getItem(position);
		
		// Initialize fragment if it hasn't been loaded.
		ViewUtils.setImageUrl(holder.pic, s.getImagePreviewUrl());

		return convertView;
	}
}
