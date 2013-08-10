package com.debian.debiandroid;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.debian.debiandroid.apiLayer.PTS;
import com.debian.debiandroid.apiLayer.SearchCacher;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class PTSFragment extends ItemDetailFragment {

	private ImageButton searchButton;
	private EditText ptsInput;
	private PTS pts;
		
	/** ID for the (un)subscribe menu item. It starts from +2 
	 * because the settings icon is in the +1 position */
	public static final int SUBSCRIPTION_ID = Menu.FIRST+2;
	public static final int REFRESH_ID = Menu.FIRST+3;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pts = new PTS(getSherlockActivity().getApplicationContext());
        if(SearchCacher.hasLastSearch()) {
        	//TODO fill all UI elements with values from last search
        }
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.pts_item_detail, container, false);
  		
    	getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.search_packages));
    	
    	searchButton = (ImageButton) rootView.findViewById(R.id.ptsSearchButton);
  		searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                	//DO SEARCH USING APILAYER, DISPLAY RESULTS
            }
        });
  		
  		ptsInput = (EditText) rootView.findViewById(R.id.ptsInputSearch);
  		ptsInput.setOnEditorActionListener(new OnEditorActionListener() {
  		    @Override
  		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
  		        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
  		        	//DO SEARCH USING APILAYER, DISPLAY RESULTS
  		            return true;
  		        }
  		        return false;
  		    }
  		});
    	
        return rootView;
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		//Add subscription icon
		MenuItem subMenuItem = menu.add(0, SUBSCRIPTION_ID, Menu.CATEGORY_SECONDARY, "(Un)Subscribe");
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		if(pts.isSubscribedTo("testpckgname")) {
			subMenuItem.setIcon(R.drawable.subscribed);
			subMenuItem.setTitle("Unsubscribe");
		} else {
			subMenuItem.setIcon(R.drawable.unsubscribed);
			subMenuItem.setTitle("Subscribe");
		}
		
		menu.add(0, REFRESH_ID, Menu.CATEGORY_ALTERNATIVE, getString(R.string.refresh))
				.setIcon(R.drawable.refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    	 switch(item.getItemId()){
		    	 case SUBSCRIPTION_ID:
			    		if(pts.isSubscribedTo("testpckgname")) {
			    			item.setIcon(R.drawable.unsubscribed);
			    			item.setTitle(getString(R.string.subscribe));
			    			pts.removeSubscriptionTo("testpckgname");
			    		} else {
			    			item.setIcon(R.drawable.subscribed);
			    			item.setTitle(getString(R.string.unsubscribe));
			    			pts.addSubscriptionTo("testpckgname");
			    		}
			    		return true;
		    	 case REFRESH_ID:
			    		// Do a refresh if there was a search
			    		return true;
	        }
		return super.onOptionsItemSelected(item);
    }
}
