package com.wao.dogcat.controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.owo.utils.Common;
import com.wao.dogcat.R;


import java.util.ArrayList;

/* 此Demo用来演示离线地图的下载和显示 */
public class OfflineMapActivity extends Activity implements MKOfflineMapListener {

    private MKOfflineMap mOffline = null;
    private TextView cidView;
    private TextView stateView;
    private EditText cityNameView;

    private ImageView btnBack;
    private TextView title;
    private LinearLayout navBar;
    private int status = -1;
    /**
     * 已下载的离线地图信息列表
     */
    private ArrayList<MKOLUpdateElement> localMapList = null;
    private LocalMapAdapter lAdapter = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
        mOffline = new MKOfflineMap();
        mOffline.init(this);
        initView();

    }

    private void initView() {

        cidView = (TextView) findViewById(R.id.cityid);
        cityNameView = (EditText) findViewById(R.id.city);
        stateView = (TextView) findViewById(R.id.state);
        navBar = (LinearLayout) findViewById(R.id.navBar);
        btnBack = (ImageView) findViewById(R.id.btnBack);
        title = (TextView) findViewById(R.id.title);
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setClass(OfflineMapActivity.this, SettingActivity.class);
                finish();
                OfflineMapActivity.this.startActivity(i);
            }
        });

        Common.userSP = getSharedPreferences("userSP", 0);
        status = Common.userSP.getInt("status", 0);

        boolean flag = Common.isNetworkAvailable(this);
        if (!flag){
            Common.display(this,"请开启手机网络");
        }

        ListView allCityList = (ListView) findViewById(R.id.allcitylist);
        // 获取所有支持离线地图的城市
        ArrayList<String> allCities = new ArrayList<String>();
        ArrayList<MKOLSearchRecord> records2 = mOffline.getOfflineCityList();
        //if (records1 != null) {
            for (MKOLSearchRecord r : records2) {
                allCities.add(r.cityName +  "   -- "
                        + this.formatDataSize(r.size));
            }
       // }
        ListAdapter aAdapter = (ListAdapter) new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, allCities);
        allCityList.setAdapter(aAdapter);

        LinearLayout cl = (LinearLayout) findViewById(R.id.citylist_layout);
        LinearLayout lm = (LinearLayout) findViewById(R.id.localmap_layout);
        lm.setVisibility(View.GONE);
        cl.setVisibility(View.VISIBLE);

        // 获取已下过的离线地图信息
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<MKOLUpdateElement>();
        }

        ListView localMapListView = (ListView) findViewById(R.id.localmaplist);
        lAdapter = new LocalMapAdapter();
        localMapListView.setAdapter(lAdapter);

    }

    /**
     * 切换至城市列表
     *
     * @param view
     */
    public void clickCityListButton(View view) {
        LinearLayout cl = (LinearLayout) findViewById(R.id.citylist_layout);
        LinearLayout lm = (LinearLayout) findViewById(R.id.localmap_layout);
        lm.setVisibility(View.GONE);
        cl.setVisibility(View.VISIBLE);

    }

    /**
     * 切换至下载管理列表
     *
     * @param view
     */
    public void clickLocalMapListButton(View view) {
        LinearLayout cl = (LinearLayout) findViewById(R.id.citylist_layout);
        LinearLayout lm = (LinearLayout) findViewById(R.id.localmap_layout);
        lm.setVisibility(View.VISIBLE);
        cl.setVisibility(View.GONE);
    }

    /**
     * 搜索离线需市
     *
     * @param view
     */
    public void search(View view) {
        ArrayList<MKOLSearchRecord> records = mOffline.searchCity(cityNameView
                .getText().toString());
        if (records == null || records.size() != 1) {
            return;
        }
        cidView.setText(String.valueOf(records.get(0).cityID));
    }

    /**
     * 开始下载
     *
     * @param view
     */
    public void start(View view) {
        int cityid = Integer.parseInt(cidView.getText().toString());
        mOffline.start(cityid);
        clickLocalMapListButton(null);
        Toast.makeText(this, "开始下载离线地图...", Toast.LENGTH_SHORT)
                .show();
        updateView();
    }

    /**
     * 暂停下载
     *
     * @param view
     */
    public void stop(View view) {
        int cityid = Integer.parseInt(cidView.getText().toString());
        mOffline.pause(cityid);
//        Toast.makeText(this, "暂停下载离线地图. cityid: " + cityid, Toast.LENGTH_SHORT)
//                .show();
        updateView();
    }

    /**
     * 删除离线地图
     *
     * @param view
     */
    public void remove(View view) {
        int cityid = Integer.parseInt(cidView.getText().toString());
        mOffline.remove(cityid);
        Toast.makeText(this, "删除成功 ", Toast.LENGTH_SHORT)
                .show();
        updateView();
    }

    /**
     * 更新状态显示
     */
    public void updateView() {
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<MKOLUpdateElement>();
        }
        lAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        int cityid = Integer.parseInt(cidView.getText().toString());
        MKOLUpdateElement temp = mOffline.getUpdateInfo(cityid);
        if (temp != null && temp.status == MKOLUpdateElement.DOWNLOADING) {
            mOffline.pause(cityid);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public String formatDataSize(int size) {
        String ret = "";
        if (size < (1024 * 1024)) {
            ret = String.format("%dK", size / 1024);
        } else {
            ret = String.format("%.1fM", size / (1024 * 1024.0));
        }
        return ret;
    }

    @Override
    protected void onDestroy() {
        /**
         * 退出时，销毁离线地图模块
         */
        mOffline.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            intent.setClass(OfflineMapActivity.this, SettingActivity.class);
            finish();
            OfflineMapActivity.this.startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onGetOfflineMapState(int type, int state) {
        switch (type) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE: {
                MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                // 处理下载进度更新提示
                if (update != null) {
                    stateView.setText(String.format("%s : %d%%", update.cityName,
                            update.ratio));
                    updateView();
                }
            }
                break;
            case MKOfflineMap.TYPE_NEW_OFFLINE:
                // 有新离线地图安装
                Log.d("OfflineDemo", String.format("add offlinemap num:%d", state));
                break;
            case MKOfflineMap.TYPE_VER_UPDATE:
                // 版本更新提示
                // MKOLUpdateElement e = mOffline.getUpdateInfo(state);

                break;
            default:
                break;
        }

    }

    /**
     * 离线地图管理列表适配器
     */
    public class LocalMapAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return localMapList.size();
        }

        @Override
        public Object getItem(int index) {
            return localMapList.get(index);
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        @Override
        public View getView(int index, View view, ViewGroup arg2) {
            MKOLUpdateElement e = (MKOLUpdateElement) getItem(index);
            view = View.inflate(OfflineMapActivity.this,
                    R.layout.offline_localmap_list, null);
            initViewItem(view, e);
            return view;
        }

        void initViewItem(View view, final MKOLUpdateElement e) {
            Button remove = (Button) view.findViewById(R.id.remove);
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView update = (TextView) view.findViewById(R.id.update);
            TextView ratio = (TextView) view.findViewById(R.id.ratio);
            ratio.setText(e.ratio + "%");
            title.setText(e.cityName);
            if (e.update) {
                update.setText("可更新");
            } else {
                update.setText("最新");
            }

            remove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mOffline.remove(e.cityID);
                    updateView();
                }
            });
        }

    }

}