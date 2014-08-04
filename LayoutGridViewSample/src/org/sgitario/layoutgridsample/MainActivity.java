package org.sgitario.layoutgridsample;

import org.sgitario.layoutgridsample.adapters.ArtbookDribbbleDataAdapter;
import org.sgitario.layoutgridsample.repository.MockRecommendationRepository;
import org.sgitario.layoutgridsample.repository.RecommendationRepository;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

	public static final String TAG = "ArtbookActivity";

	private LayoutViewGroup container;

	private RecommendationRepository fetch;
	private int itemsPerPage = 25;
	private int pageIndex = 1;
	
	ArtbookDribbbleDataAdapter adapter;
	
	int currLayoutIndex = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_artbook);

		container = (LayoutViewGroup) findViewById(R.id.container);
		
		findViewById(R.id.load_more).setOnClickListener(this);
		
		adapter = new ArtbookDribbbleDataAdapter(this);
		
		container.setAdapter(adapter);
		
		fetch = new MockRecommendationRepository();
		
		adapter.update(fetch.getTrendingTopics(pageIndex, itemsPerPage));
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "Loading data");
		pageIndex++;
		adapter.update(fetch.getTrendingTopics(pageIndex, itemsPerPage));
	}

}
