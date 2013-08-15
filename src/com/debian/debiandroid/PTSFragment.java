package com.debian.debiandroid;

import java.util.ArrayList;
import java.util.Arrays;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.debian.debiandroid.apiLayer.BTS;
import com.debian.debiandroid.apiLayer.PTS;
import com.debian.debiandroid.apiLayer.SearchCacher;
import com.debian.debiandroid.content.ContentMenu;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView.OnEditorActionListener;

public class PTSFragment extends ItemDetailFragment {

	private ImageButton searchButton;
	private EditText ptsInput;
	private PTS pts;
	private ExpandableListView bugList;
	private TextView ptsPckgName, ptsPckgLatestVersion, 
					ptsPckgMaintainerInfo, ptsPckgBugCount, 
					ptsPckgUplNames, ptsPckgBinNames;
	
	private ArrayList<String> parentItems;
	private ArrayList<Object> childItems;
	
	/** ID for the (un)subscribe menu item. It starts from +2 
	 * because the settings icon is in the +1 position */
	public static final int SUBSCRIPTION_ID = Menu.FIRST+2;
	public static final int REFRESH_ID = Menu.FIRST+3;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pts = new PTS(getSherlockActivity().getApplicationContext());
        if(SearchCacher.hasLastSearch()) {
        	new SearchPackageInfoTask().execute();
        }
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.pts_item_detail, container, false);

    	bugList = (ExpandableListView) rootView.findViewById(R.id.ptsBugslist);
    	ViewGroup header = (ViewGroup)inflater.inflate(R.layout.pts_item_header, bugList, false);
    	bugList.addHeaderView(header, null, false);
    	
    	getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.search_packages));
    	
    	searchButton = (ImageButton) rootView.findViewById(R.id.ptsSearchButton);
    	ptsInput = (EditText) rootView.findViewById(R.id.ptsInputSearch);
    	ptsPckgName = (TextView) rootView.findViewById(R.id.ptsPckgName);
    	ptsPckgLatestVersion = (TextView) rootView.findViewById(R.id.ptsPckgLatestVersion);
    	ptsPckgMaintainerInfo = (TextView) rootView.findViewById(R.id.ptsPckgMaintainerInfo);
    	ptsPckgBugCount = (TextView) rootView.findViewById(R.id.ptsPckgBugCount);
    	ptsPckgUplNames = (TextView) rootView.findViewById(R.id.ptsPckgUplNames);
    	ptsPckgBinNames = (TextView) rootView.findViewById(R.id.ptsPckgBinNames);
    	
  		searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	SearchCacher.setLastSearchByPckgName(ptsInput.getText().toString().trim());
            	new SearchPackageInfoTask().execute();
            }
        });
  		
  		ptsInput.setOnEditorActionListener(new OnEditorActionListener() {
  		    @Override
  		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
  		        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
  		        	SearchCacher.setLastSearchByPckgName(ptsInput.getText().toString().trim());
  		        	new SearchPackageInfoTask().execute();
  		            return true;
  		        }
  		        return false;
  		    }
  		});
    	
        return rootView;
    }
	
	public void setupBugsList() {    	
		bugList.setDividerHeight(1);
		bugList.setClickable(true);
    	
    	final DExpandableAdapter adapter = new DExpandableAdapter(parentItems, childItems);
    	adapter.setInflater((LayoutInflater) getSherlockActivity()
    						.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    	bugList.setAdapter(adapter);
    	registerForContextMenu(bugList);
    	
    	bugList.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View view,
                    int groupPosition, int childPosition, long id) {
            	String itemClicked = ((TextView)view).getText().toString();
                System.out.println("Child Clicked " + itemClicked + " " + groupPosition);
                //save search by bug num
                SearchCacher.setLastSearchByBugNumber(itemClicked);
                // Move to bts fragment
      		  	ItemDetailFragment fragment = ItemDetailFragment.getDetailFragment(
      				  ContentMenu.ITEM.BTS.toString());
          		getActivity().getSupportFragmentManager().beginTransaction()
              	.replace(R.id.item_detail_container, fragment)
              	.commit();
                return true;
            }
        });
	}
	
	public void setBugData(String pkgName) {
		parentItems = new ArrayList<String>();
		childItems = new ArrayList<Object>();
		
		Context context = getSherlockActivity().getApplicationContext();
		BTS bts = new BTS(context);
		
		ArrayList<String> bugs = bts.getBugs(new String[]{BTS.PACKAGE}, new String[]{pkgName});
		parentItems.add(getString(R.string.all_bugs) + " (" + bugs.size() + ")");
	    childItems.add(bugs);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		//Add subscription icon
		MenuItem subMenuItem = menu.add(0, SUBSCRIPTION_ID, Menu.CATEGORY_SECONDARY, "(Un)Subscribe");
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	 	setSubscriptionIcon(subMenuItem, SearchCacher.getLastPckgName());
		
		menu.add(0, REFRESH_ID, Menu.CATEGORY_ALTERNATIVE, getString(R.string.refresh))
				.setIcon(R.drawable.refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu){
		setSubscriptionIcon(menu.findItem(SUBSCRIPTION_ID), SearchCacher.getLastPckgName());
		super.onPrepareOptionsMenu(menu);
	}

	public void setSubscriptionIcon(MenuItem subMenuItem, String pckgName) {
		if(pckgName!=null && pts.isSubscribedTo(pckgName)) {
			subMenuItem.setIcon(R.drawable.subscribed);
			subMenuItem.setTitle(getString(R.string.unsubscribe));
		} else {
			subMenuItem.setIcon(R.drawable.unsubscribed);
			subMenuItem.setTitle(getString(R.string.subscribe));
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    	 switch(item.getItemId()){
		    	 case SUBSCRIPTION_ID:
		    		 	String pckgName = SearchCacher.getLastPckgName();
		    		 	if(pckgName!=null) {
				    		if(pts.isSubscribedTo(pckgName)) {
				    			item.setIcon(R.drawable.unsubscribed);
				    			item.setTitle(getString(R.string.subscribe));
				    			pts.removeSubscriptionTo(pckgName);
				    		} else {
				    			item.setIcon(R.drawable.subscribed);
				    			item.setTitle(getString(R.string.unsubscribe));
				    			pts.addSubscriptionTo(pckgName);
				    		}
				    	}
			    		return true;
		    	 case REFRESH_ID:
		    		 	new SearchPackageInfoTask().execute();
			    		return true;
	        }
		return super.onOptionsItemSelected(item);
    }
	
	class SearchPackageInfoTask extends AsyncTask<Void, Void, Void> {
		private String[] pckgInfo;
		private ProgressDialog progressDialog;
		protected void onPreExecute(){ 
		   super.onPreExecute();
		   progressDialog = ProgressDialog.show(getSherlockActivity(), 
				   getString(R.string.searching), getString(R.string.searching_info_about) + " " + SearchCacher.getLastPckgName() 
				   + ". " + getString(R.string.please_wait) + "...", true, false);  
		}
		
		protected Void doInBackground(Void... params) {
			pckgInfo = new String[6];
			pckgInfo[0] = SearchCacher.getLastPckgName(); //Last Package Name
			pckgInfo[1] = pts.getLatestVersion(pckgInfo[0]);
			pckgInfo[2] = pts.getMaintainerName(pckgInfo[0]) + "\n <" + pts.getMaintainerEmail(pckgInfo[0])+ ">";
			pckgInfo[3] = pts.getBugCounts(pckgInfo[0]);
			pckgInfo[4] = Arrays.toString(pts.getUploaderNames(pckgInfo[0]));
			pckgInfo[5] = Arrays.toString(pts.getBinaryNames(pckgInfo[0]));
			setBugData(pckgInfo[0]);
			return null;
		}  
		@SuppressLint("NewApi")
		protected void onPostExecute (Void result) {
			progressDialog.dismiss();
			ptsInput.setText(pckgInfo[0]);
			ptsPckgName.setText(getString(R.string.pckg) + ": \n  "+ pckgInfo[0]);
			ptsPckgLatestVersion.setText(getString(R.string.latest_version) + ": \n  " + pckgInfo[1]);
			ptsPckgMaintainerInfo.setText(getString(R.string.maintainer) + ": \n  " + pckgInfo[2]);
			ptsPckgBugCount.setText(getString(R.string.bug_count) + ": \n  " + pckgInfo[3]);
			ptsPckgUplNames.setText(getString(R.string.uploaders) + ": \n" + pckgInfo[4]);
	    	ptsPckgBinNames.setText(getString(R.string.binary_names) + ": \n" + pckgInfo[5]);
	    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    		getSherlockActivity().invalidateOptionsMenu();
			}
	    	setupBugsList();
	    }
    }
}
