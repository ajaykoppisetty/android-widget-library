package com.example.custom.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;

import com.thecamtech.andoird.library.widget.SwapeRefreshListView;
import com.thecamtech.andoird.library.widget.SwapeRefreshListView.OnRefreshListener;

public class MainActivity extends Activity {

	private SwapeRefreshListView mListView;
	private String[] data;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		data = new String[300];
		for (int i = 0; i < 300; i++) {
			data[i] = "Testing number " + i;
		}

		mListView = (SwapeRefreshListView) findViewById(R.id.listview);
		mListView.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, data));

		mListView.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public boolean onRefresh(View view) {
				view.postDelayed(new Runnable() {
					@Override
					public void run() {
						mListView.doneLoading();
					}
				}, 3000);
				// return true if we want horizontal progress bar appear
				// otherwise return false the you must handle progressing on
				// your own view;
				// or simply not pass progressBar attribute in your layout.
				return true;
			}

			@Override
			public void onBeginDrag(View view) {

			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
