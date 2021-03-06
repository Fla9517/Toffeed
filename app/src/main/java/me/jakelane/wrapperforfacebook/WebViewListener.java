package me.jakelane.wrapperforfacebook;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.webkit.WebView;

import com.github.clans.fab.FloatingActionMenu;

import im.delight.android.webview.AdvancedWebView;

class WebViewListener implements AdvancedWebView.Listener {
    // *{-webkit-tap-highlight-color: rgba(0,0,0, 0.0);outline: none;}
    private static final String HIDE_ORANGE_FOCUS = "*%7B-webkit-tap-highlight-color%3Atransparent%3Boutline%3A0%7D";
    // #page{top:-45px;}
    private static final String HIDE_MENU_BAR_CSS = "%23page%7Btop%3A-45px%7D";
    // #mbasic_inline_feed_composer{display:none}
    private static final String HIDE_COMPOSER_CSS = "%23mbasic_inline_feed_composer%7Bdisplay%3Anone%7D";
    // article[data-ft*=ei]{display:none;}
    private static final String HIDE_SPONSORED = "article%5Bdata-ft*%3Dei%5D%7Bdisplay%3Anone%7D";

    private final MainActivity mActivity;
    private final SharedPreferences mPreferences;
    private final AdvancedWebView mWebView;
    private final FloatingActionMenu mMenuFAB;
    private final int mScrollThreshold;

    WebViewListener(MainActivity activity, WebView view) {
        mActivity = activity;
        mWebView = (AdvancedWebView) view;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        mScrollThreshold = activity.getResources().getDimensionPixelOffset(R.dimen.fab_scroll_threshold);
        mMenuFAB = (FloatingActionMenu) activity.findViewById(R.id.menuFAB);
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        // Show the spinner and hide the WebView
        mActivity.setLoading(true);
    }

    @Override
    public void onPageFinished(String url) {
        Uri uri = Uri.parse(url);
        // Only do things if logged in
        if (mActivity.checkLoggedInState()) {
            // Load a certain page if there is a parameter
            JavaScriptHelpers.paramLoader(mWebView, url);

            // Hide Orange highlight on focus
            String css = HIDE_ORANGE_FOCUS;

            // Hide the menu bar (but not on the composer)
            if (!url.contains("/composer/")) {
                css += HIDE_MENU_BAR_CSS;
                mActivity.swipeView.setEnabled(true);
            } else {
                mActivity.swipeView.setEnabled(false);
            }

            if (uri.getPath().equals("/") || uri.getPath().equals("/home.php")) {
                JavaScriptHelpers.mostRecentButton(mWebView);
            }

            // Hide the status editor on the News Feed if setting is enabled
            if (mPreferences.getBoolean(SettingsActivity.KEY_PREF_HIDE_EDITOR, true)) {
                css += HIDE_COMPOSER_CSS;
            }

            // Hide 'Sponsored' content (ads)
            if (mPreferences.getBoolean(SettingsActivity.KEY_PREF_HIDE_SPONSORED, true)) {
                css += HIDE_SPONSORED;
            }

            // Inject the css
            JavaScriptHelpers.loadCSS(mWebView, css);

            // Get the currently open tab and check on the navigation menu
            JavaScriptHelpers.updateCurrentTab(mWebView);

            // Get the notification number
            JavaScriptHelpers.updateNotificationsService(mWebView);

            // Get the messages number
            if (mPreferences.getBoolean(SettingsActivity.KEY_PREF_MESSAGING, false)) {
                JavaScriptHelpers.updateMessagesService(mWebView);
            }
        }
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        mActivity.setLoading(false);
    }

    @Override
    public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
    }

    @Override
    public void onExternalPageRequest(String url) {
        Log.v(Helpers.LogTag, "External page: " + url);
        // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        intentBuilder.setShowTitle(true);
        intentBuilder.setToolbarColor(ContextCompat.getColor(mActivity, R.color.colorPrimary));

        Intent actionIntent = new Intent(Intent.ACTION_SEND);
        actionIntent.setType("text/plain");
        actionIntent.putExtra(Intent.EXTRA_TEXT, url);

        PendingIntent menuItemPendingIntent = PendingIntent.getActivity(mActivity, 0, actionIntent, 0);
        intentBuilder.addMenuItem(mActivity.getString(R.string.share_text), menuItemPendingIntent);

        intentBuilder.build().launchUrl(mActivity, Uri.parse(url));
    }

    @Override
    public void onScrollChange(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        // Make sure the hiding is enabled and the scroll was significant
        if (mPreferences.getBoolean(SettingsActivity.KEY_PREF_FAB_SCROLL, false) && Math.abs(oldScrollY - scrollY) > mScrollThreshold) {
            if (scrollY > oldScrollY) {
                // User scrolled down, hide the button
                mMenuFAB.hideMenuButton(true);
            } else if (scrollY < oldScrollY) {
                // User scrolled up, show the button
                mMenuFAB.showMenuButton(true);
            }

        }
    }
}
