package com.owo.module_b_message.controller;

import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.owo.module_b_message.view.ConversationListView;
import com.owo.module_b_message.widgets.FragMessage;
import com.wao.dogcat.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.GroupInfo;
import cn.jpush.im.android.api.model.UserInfo;
import io.jchat.android.activity.ConversationListFragment;
import io.jchat.android.adapter.ConversationListAdapter;
import io.jchat.android.application.JChatDemoApplication;
import io.jchat.android.chatting.ChatActivity;
import io.jchat.android.chatting.utils.DialogCreator;
import io.jchat.android.tools.SortConvList;

public class ConversationListController implements OnClickListener,
        OnItemClickListener, OnItemLongClickListener {

    private ConversationListView mConvListView;
    private FragMessage mContext;
    private List<Conversation> mDatas = new ArrayList<Conversation>();
    private ConversationListAdapter mListAdapter;
    private int mWidth;
    private Dialog mDialog;

    public ConversationListController(ConversationListView listView, FragMessage context,
                                      int width) {
        this.mConvListView = listView;
        this.mContext = context;
        this.mWidth = width;
        initConvListAdapter();
    }

    // 得到会话列表
    private void initConvListAdapter() {
        mDatas = JMessageClient.getConversationList();
        //对会话列表进行时间排序
        if (mDatas.size() > 1) {
            SortConvList sortList = new SortConvList();
            Collections.sort(mDatas, sortList);
        }

        mListAdapter = new ConversationListAdapter(mContext.getActivity(), mDatas);
        mConvListView.setConvListAdapter(mListAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_group_btn:
                mContext.showMenuPopWindow();
                break;
        }
    }

    // 点击会话列表
    @Override
    public void onItemClick(AdapterView<?> viewAdapter, View view, int position, long id) {
        // TODO Auto-generated method stub
        final Intent intent = new Intent();
        try {
            if (position > 0) {
                // 减去 header view 个数
                Conversation conv = mDatas.get(position - 2);
                intent.putExtra(JChatDemoApplication.CONV_TITLE, conv.getTitle());
                if (null != conv) {
                    // 当前点击的会话是否为群组
                    if (conv.getType() == ConversationType.group) {
                        if (mListAdapter.includeAtMsg(conv)) {
                            intent.putExtra("atMsgId", mListAdapter.getAtMsgId(conv));
                        }
                        long groupId = ((GroupInfo) conv.getTargetInfo()).getGroupID();
                        intent.putExtra(JChatDemoApplication.GROUP_ID, groupId);
                        intent.putExtra(JChatDemoApplication.DRAFT, getAdapter().getDraft(conv.getId()));
                        intent.setClass(mContext.getActivity(), ChatActivity.class);
                        mContext.getActivity().startActivity(intent);
                        return;
                    } else {
                        String targetId = ((UserInfo) conv.getTargetInfo()).getUserName();
                        intent.putExtra(JChatDemoApplication.TARGET_ID, targetId);
                        intent.putExtra(JChatDemoApplication.TARGET_APP_KEY, conv.getTargetAppKey());
                        Log.d("ConversationList", "Target app key from conversation: " + conv.getTargetAppKey());
                        intent.putExtra(JChatDemoApplication.DRAFT, getAdapter().getDraft(conv.getId()));
                    }
                    intent.setClass(mContext.getActivity(), ChatActivity.class);
                    mContext.getActivity().startActivity(intent);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> viewAdapter, View view, final int position, long id) {
        if (position > 0) {
            try {
                final Conversation conv = mDatas.get(position - 2);
                if (conv != null) {
                    OnClickListener listener = new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (conv.getType() == ConversationType.group) {
                                JMessageClient.deleteGroupConversation(((GroupInfo) conv.getTargetInfo())
                                        .getGroupID());
                            } else {
                                //使用带AppKey的接口,可以删除跨/非跨应用的会话(如果不是跨应用,conv拿到的AppKey则是默认的)
                                JMessageClient.deleteSingleConversation(((UserInfo) conv.getTargetInfo())
                                        .getUserName(), conv.getTargetAppKey());
                            }
                            mDatas.remove(position - 2);
                            mListAdapter.notifyDataSetChanged();
                            mDialog.dismiss();
                        }
                    };
                    mDialog = DialogCreator.createDelConversationDialog(mContext.getActivity(), conv.getTitle(),
                            listener);
                    mDialog.show();
                    mDialog.getWindow().setLayout((int) (0.8 * mWidth), WindowManager.LayoutParams.WRAP_CONTENT);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public ConversationListAdapter getAdapter() {
        return mListAdapter;
    }

}
