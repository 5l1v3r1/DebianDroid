
package net.debian.debiandroid;

import net.debian.debiandroid.apiLayer.BTS;
import net.debian.debiandroid.apiLayer.PTS;
import net.debian.debiandroid.contentfragments.ContentHelper;
import net.debian.debiandroid.utils.SearchCacher;
import net.debian.debiandroid.utils.SwipeDetector;
import net.debian.debiandroid.utils.UIUtils;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GestureDetectorCompat;
import android.view.MotionEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.cketti.library.changelog.ChangeLog;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details
 * (if present) is a {@link ItemFragment}.
 * <p>
 * This activity also implements the required
 * {@link ItemListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ItemListActivity extends SherlockFragmentActivity implements ItemListFragment.Callbacks {

    private GestureDetectorCompat gestureDetector;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private static boolean animateToLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        // Initialize content menu elements
        ContentHelper.initializeItems(getApplicationContext());

        gestureDetector = new GestureDetectorCompat(this, new ListSwipeListener());

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list))
                    .setActivateOnItemClick(true);

            animateToLeft = false;
            onItemSelected(ContentHelper.PTS);
        }

        //new task().execute(); //temporary used for testing api methods

        // Load stored settings
        SettingsActivity.loadSettings(getApplicationContext());

        // Start service that auto updates subscribed packages and notifies user
        /*PendingIntent pintent = PendingIntent.getService(this, 0, new Intent(this, DDNotifyService.class), 0);
        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        // Start service again every 300 seconds
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), 300*1000, pintent);
        */

        // Show changelog if it's the first time a new version loads
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }

        // Check if app opened links to bugs.debian.org or packages.qa.debian.org
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();

            // Parse uri to get search parameters and forward to corresponding fragment
            if (PTS.isPTSHost(uri.getHost())) {
                SearchCacher.setLastSearchByPckgName(PTS.PTSURIToPckgName(uri));
                onItemSelected(ContentHelper.PTS);
            }
            if (BTS.isBTSHost(uri.getHost())) {
                uri = Uri.parse(uri.toString().replace(';', '&'));
                SearchCacher.setLastSearchByBTSURI(uri);
                onItemSelected(ContentHelper.BTS);
            }
        }
    }

    class task extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            System.out.println(new PTS(getApplicationContext()).getMadisonInfo("vim"));

            return null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        DDNotifyService.activityPaused();
    }

    @Override
    public void onResume() {
        super.onResume();
        DDNotifyService.activityResumed();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return gestureDetector.onTouchEvent(ev);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (mTwoPane) {
            //Forward the qrcode scan result to the corresponding CIFFragment
            ItemFragment fragment = (ItemFragment) getSupportFragmentManager().findFragmentById(
                    R.id.item_detail_container);
            if ((fragment != null) && fragment.isAdded() && ItemFragment.currentFragID.equals(ContentHelper.CIF)) {
                fragment.onActivityResult(requestCode, resultCode, intent);
            }
        }
    }

    /**
     * Callback method from {@link ItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected
     * and that the user will be forwarded to the appropriate fragment
     * or ItemDetailActivity.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (ItemFragment.isInListDisplayFrag) {
                FragmentManager fm = getSupportFragmentManager();
                if (fm != null) {
                    fm.popBackStack();
                }
            }

            Bundle arguments = new Bundle();
            arguments.putString(ItemFragment.ARG_ITEM_ID, id);
            UIUtils.loadFragment(getSupportFragmentManager(), id, arguments, animateToLeft);
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ItemDetailActivity.class);
            detailIntent.putExtra(ItemFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
        animateToLeft = false;
    }

    class ListSwipeListener extends SwipeDetector {

        @Override
        public boolean onSwipeRight() {
            super.onSwipeRight();
            String fragmentID = ContentHelper.getPreviousFragmentId();
            if (fragmentID != null) {
                animateToLeft = false;
                onItemSelected(fragmentID);
            }
            return true;
        }

        @Override
        public boolean onSwipeLeft() {
            super.onSwipeLeft();
            String fragmentID = ContentHelper.getNextFragmentId();
            if (fragmentID != null) {
                animateToLeft = true;
                onItemSelected(fragmentID);
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            if (ItemFragment.isInListDisplayFrag) {
                return false;
            }

            return super.onFling(event1, event2, velocityX, velocityY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mTwoPane) {
            UIUtils.addSettingsMenuItem(menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mTwoPane) {
            switch (item.getItemId()) {
                case ItemFragment.SETTINGS_ID:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
