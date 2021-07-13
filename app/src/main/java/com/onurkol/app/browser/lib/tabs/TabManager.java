package com.onurkol.app.browser.lib.tabs;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onurkol.app.browser.R;
import com.onurkol.app.browser.activity.MainActivity;
import com.onurkol.app.browser.data.tabs.ClassesTabData;
import com.onurkol.app.browser.data.tabs.IncognitoTabData;
import com.onurkol.app.browser.data.tabs.TabData;
import com.onurkol.app.browser.fragments.tabs.IncognitoTabFragment;
import com.onurkol.app.browser.fragments.tabs.TabFragment;
import com.onurkol.app.browser.interfaces.tabs.TabManagers;
import com.onurkol.app.browser.interfaces.tabs.TabSettings;
import com.onurkol.app.browser.lib.AppPreferenceManager;
import com.onurkol.app.browser.lib.ContextManager;
import com.onurkol.app.browser.tools.ProcessDelay;
import com.onurkol.app.browser.webview.OKWebView;

import java.util.ArrayList;

public class TabManager implements TabSettings, TabManagers {
    // Variables
    AppPreferenceManager prefManager;
    Context context;
    FragmentManager fragmentManager;

    // Tab Manager Objects
    private OKWebView activeTabWebView=null, activeIncognitoWebView=null;
    private TabFragment activeTabFragment=null;
    private IncognitoTabFragment activeIncognitoTabFragment=null;
    private int activeTabPosition, activeIncognitoTabPosition;

    Gson gson=new Gson();

    public void BuildManager(){
        prefManager=AppPreferenceManager.getInstance();
        context=ContextManager.getManager().getContext();
        fragmentManager=((FragmentActivity)context).getSupportFragmentManager();
        // Init Preference Data
        initTabPreferenceData();
    }

    private void initTabPreferenceData(){
        // Check Preference Load
        if(prefManager.getString(KEY_TAB_PREFERENCE)==null)
            prefManager.setPreference(KEY_TAB_PREFERENCE,"");
    }

    @Override
    public void createNewTab() {
        createNewTabHandler("");
    }
    @Override
    public void createNewTab(String Url) {
        createNewTabHandler(Url);
    }
    private void createNewTabHandler(String getUrl){
        // Remove Active Incognito Tabs
        setActiveIncognitoFragment(null);
        // Fragment Bundle
        Bundle bundle = new Bundle();
        // Put Send Url
        bundle.putString(KEY_TAB_URL_SENDER,getUrl);
        // Create Tab Fragment
        TabFragment newTabFragment=new TabFragment();
        // Add View
        addFragmentView(R.id.browserFragmentView, newTabFragment);
        // Set Bundle
        newTabFragment.setArguments(bundle);
        // Set Tab Index
        newTabFragment.setTabIndex(BROWSER_TABDATA_LIST.size());
        // Default Tab Title
        String tabTitle=context.getString(R.string.loading_text)+" ...";
        // Tab Current Data
        TabData data=new TabData(tabTitle,"");
        ClassesTabData classesTabData=new ClassesTabData(newTabFragment,null);
        // Adding Tab List
        BROWSER_TAB_FRAGMENT_LIST.add(newTabFragment);
        BROWSER_TABDATA_LIST.add(data);
        BROWSER_CLASSES_TABDATA_LIST.add(classesTabData);

        // Tab Saved to Preference
        saveTabListPreference(BROWSER_TABDATA_LIST);

        // Show New Tab Fragment
        for (int i=0; i < BROWSER_TAB_FRAGMENT_LIST.size(); i++) {
            if(BROWSER_TAB_FRAGMENT_LIST.get(i)==newTabFragment) {
                showFragment(newTabFragment);
                // Set Active Fragment
                setActiveTabFragment(newTabFragment);
            }
            else
                if(isShowingFragment(BROWSER_TAB_FRAGMENT_LIST.get(i)))
                    hideFragment(BROWSER_TAB_FRAGMENT_LIST.get(i));
        }
        // Hide All Incognito Tabs
        hideAllIncognitoTabs();
    }

    @Override
    public void changeTab(int tabIndex) {
        // Remove Active Incognito Tabs
        setActiveIncognitoFragment(null);
        // Get Selected Fragment
        TabFragment selectFragment=BROWSER_TAB_FRAGMENT_LIST.get(tabIndex);
        // Set Tab Fragment
        setActiveTabFragment(selectFragment);

        // Change Fragment View
        int fragList=BROWSER_TAB_FRAGMENT_LIST.size();
        for (int i=0; i < fragList; i++) {
            if(BROWSER_TAB_FRAGMENT_LIST.get(i)==selectFragment){
                showFragment(selectFragment);
                // Call Fragment Data Refresh
                selectFragment.refreshLoadView();
                // Resume WebView
                selectFragment.getWebView().onResume();
            }
            else {
                // Hide Fragments
                hideFragment(BROWSER_TAB_FRAGMENT_LIST.get(i));
                // Pause WebView
                BROWSER_TAB_FRAGMENT_LIST.get(i).getWebView().onPause();
            }
        }
        // Hide All Incognito Tabs
        hideAllIncognitoTabs();
    }

    private void hideAllTabs(){
        // Hide All Normal Tabs
        for(int i=0; i<BROWSER_TAB_FRAGMENT_LIST.size(); i++){
            if(isShowingFragment(BROWSER_TAB_FRAGMENT_LIST.get(i))) {
                hideFragment(BROWSER_TAB_FRAGMENT_LIST.get(i));
                // Pause WebViews
                BROWSER_TAB_FRAGMENT_LIST.get(i).getWebView().onPause();
            }
        }
    }

    private void hideAllIncognitoTabs(){
        // Hide All Incognito Tabs
        for(int i=0; i<BROWSER_INCOGNITO_FRAGMENT_LIST.size(); i++){
            if(isShowingFragment(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i))) {
                hideFragment(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i));
                // Pause WebViews
                BROWSER_INCOGNITO_FRAGMENT_LIST.get(i).getWebView().onPause();
            }
        }
    }

    @Override
    public void setActiveTabFragment(TabFragment tabFragment) {
        activeTabFragment=tabFragment;
    }

    @Override
    public TabFragment getActiveTabFragment() {
        return activeTabFragment;
    }

    @Override
    public ArrayList<TabFragment> getTabFragmentList() {
        return BROWSER_TAB_FRAGMENT_LIST;
    }

    @Override
    public ArrayList<TabData> getTabDataList() {
        return BROWSER_TABDATA_LIST;
    }

    @Override
    public ArrayList<ClassesTabData> getClassesTabDataList() {
        return BROWSER_CLASSES_TABDATA_LIST;
    }

    @Override
    public void recreateTabIndex() {
        int tabCount=BROWSER_TAB_FRAGMENT_LIST.size();

        for(int i=0; i<tabCount; i++)
            BROWSER_TAB_FRAGMENT_LIST.get(i).setTabIndex(i);
    }

    @Override
    public void saveTabListPreference(ArrayList<TabData> tabDataList) {
        int listSize=tabDataList.size();
        // Variables
        String newData;
        if(listSize>0) {
            // Added New Data
            StringBuilder processData = new StringBuilder();
            for (int i = 0; i < listSize; i++) {
                if (i == (listSize - 1))
                    processData.append(gson.toJson(tabDataList.get(i)));
                else
                    processData.append(gson.toJson(tabDataList.get(i))).append(",");
            }
            newData = "[" + processData + "]";
        }
        else
            // Clear All Tabs
            newData="";
        // Save Preference
        prefManager.setPreference(KEY_TAB_PREFERENCE,newData);
    }

    @Override
    public ArrayList<TabData> getSavedTabList() {
        // New Data
        ArrayList<TabData> savedTabsList=new ArrayList<>();
        // Get Preference
        String savedTabsString=prefManager.getString(KEY_TAB_PREFERENCE);
        if(!savedTabsString.equals(""))
            // Convert String to List
            savedTabsList=gson.fromJson(savedTabsString, new TypeToken<ArrayList<TabData>>(){}.getType());
        return savedTabsList;
    }

    @Override
    public void syncSavedTabs() {
        // Clear Tab Data
        BROWSER_TABDATA_LIST.clear();
        BROWSER_CLASSES_TABDATA_LIST.clear();
        BROWSER_TAB_FRAGMENT_LIST.clear();
        // SYNC
        BROWSER_TABDATA_LIST.addAll(getSavedTabList());
        // SYNC Fragments (create fragments & add views)
        int tabDataCount=getSavedTabList().size();
        for (int i=0; i<tabDataCount; i++) {
            // Create Fragments
            TabFragment newTabDataFragment=new TabFragment();
            // Set Fragment Data
            newTabDataFragment.setTabIndex(i);
            // Add Views
            addFragmentView(R.id.browserFragmentView, newTabDataFragment);
            // Hide Fragments
            if(i==0) {
                // Call Fragment Data Refresh
                ProcessDelay.Delay(() -> newTabDataFragment.refreshLoadView(), 100);
                // Set Active Tab
                setActiveTabFragment(newTabDataFragment);
            }
            else
            if(isShowingFragment(newTabDataFragment))
                hideFragment(newTabDataFragment);
            // Add List
            BROWSER_TAB_FRAGMENT_LIST.add(newTabDataFragment);
            // Tab Classes Data
            BROWSER_CLASSES_TABDATA_LIST.add(new ClassesTabData(newTabDataFragment,null));
        }
    }

    @Override
    public void updateSyncTabData(int position, TabData updateData, ClassesTabData updateClassesData) {
        if(BROWSER_TABDATA_LIST.size()>0) {
            // Remove Old Datas
            BROWSER_TABDATA_LIST.remove(position);
            BROWSER_CLASSES_TABDATA_LIST.remove(position);
            // Add New Data in Index
            BROWSER_TABDATA_LIST.add(position, updateData);
            // Update Tab Classes Data
            BROWSER_CLASSES_TABDATA_LIST.add(position, updateClassesData);
            // Save New Preference
            saveTabListPreference(BROWSER_TABDATA_LIST);
        }
    }

    @Override
    public void createNewIncognitoTab() {
        createNewIncognitoTabHandler("");
    }
    @Override
    public void createNewIncognitoTab(String Url) {
        createNewIncognitoTabHandler(Url);
    }

    private void createNewIncognitoTabHandler(String getUrl){
        // Remove Active Tabs
        setActiveTabFragment(null);
        // Fragment Bundle
        Bundle bundle = new Bundle();
        // Put Send Url
        bundle.putString(KEY_TAB_URL_SENDER,getUrl);
        // Create Tab Fragment
        IncognitoTabFragment newIncognitoTabFragment=new IncognitoTabFragment();
        // Add View
        addFragmentView(R.id.browserFragmentView, newIncognitoTabFragment);
        // Set Bundle
        newIncognitoTabFragment.setArguments(bundle);
        // Set Tab Index
        newIncognitoTabFragment.setActiveTabIndex(BROWSER_INCOGNITO_TABDATA_LIST.size());
        // Default Tab Title
        String tabTitle=context.getString(R.string.loading_text)+" ...";
        // Tab Current Data
        IncognitoTabData data=new IncognitoTabData(tabTitle,"", newIncognitoTabFragment, null);
        // Adding Tab List
        BROWSER_INCOGNITO_FRAGMENT_LIST.add(newIncognitoTabFragment);
        BROWSER_INCOGNITO_TABDATA_LIST.add(data);
        // Show New Tab Fragment
        for (int i=0; i < BROWSER_INCOGNITO_FRAGMENT_LIST.size(); i++) {
            if(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i)==newIncognitoTabFragment) {
                showFragment(newIncognitoTabFragment);
                // Set Tab Fragment
                setActiveIncognitoFragment(newIncognitoTabFragment);
            }
            else
                if(isShowingFragment(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i)))
                    hideFragment(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i));
        }
        // Hide All Tabs
        hideAllTabs();
    }

    @Override
    public void changeIncognitoTab(int tabIndex) {
        // Remove Active Incognito Tabs
        setActiveTabFragment(null);
        // Get Selected Fragment
        IncognitoTabFragment selectFragment=BROWSER_INCOGNITO_FRAGMENT_LIST.get(tabIndex);
        // Set Tab Fragment
        setActiveIncognitoFragment(selectFragment);

        // Change Fragment View
        int fragList=BROWSER_INCOGNITO_FRAGMENT_LIST.size();
        for (int i=0; i < fragList; i++) {
            if(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i)==selectFragment){
                showFragment(selectFragment);
                // Resume WebView
                selectFragment.getWebView().onResume();
            }
            else {
                // Hide Fragments
                hideFragment(BROWSER_INCOGNITO_FRAGMENT_LIST.get(i));
                // Pause WebView
                BROWSER_INCOGNITO_FRAGMENT_LIST.get(i).getWebView().onPause();
            }
        }
        // Hide All Incognito Tabs
        hideAllTabs();
    }

    @Override
    public void setActiveIncognitoFragment(IncognitoTabFragment incognitoFragment) {
        activeIncognitoTabFragment=incognitoFragment;
    }

    @Override
    public IncognitoTabFragment getActiveIncognitoFragment() {
        return activeIncognitoTabFragment;
    }

    @Override
    public ArrayList<IncognitoTabFragment> getIncognitoTabFragmentList() {
        return BROWSER_INCOGNITO_FRAGMENT_LIST;
    }

    @Override
    public ArrayList<IncognitoTabData> getIncognitoTabDataList() {
        return BROWSER_INCOGNITO_TABDATA_LIST;
    }

    // Fragments
    @Override
    public void addFragmentView(int viewId, Fragment fragment) {
        fragmentManager.beginTransaction().add(viewId, fragment).commit();
    }
    @Override
    public void removeFragment(Fragment fragment) {
        fragmentManager.beginTransaction().remove(fragment).commit();
    }
    @Override
    public void showFragment(Fragment fragment) {
        fragmentManager.beginTransaction().show(fragment).commit();
    }
    @Override
    public void hideFragment(Fragment fragment) {
        fragmentManager.beginTransaction().hide(fragment).commit();
    }
    @Override
    public void attachFragment(Fragment fragment) {
        fragmentManager.beginTransaction().attach(fragment).commit();
    }
    @Override
    public void detachFragment(Fragment fragment) {
        fragmentManager.beginTransaction().detach(fragment).commit();
    }

    @Override
    public boolean isShowingFragment(Fragment fragment) {
        return !fragment.isHidden();
    }
    @Override
    public boolean isHiddenFragment(Fragment fragment) {
        return fragment.isHidden();
    }
    @Override
    public boolean isAttachedFragment(Fragment fragment) {
        return !fragment.isDetached();
    }
    @Override
    public boolean isDetachedFragment(Fragment fragment) {
        return fragment.isDetached();
    }
}
