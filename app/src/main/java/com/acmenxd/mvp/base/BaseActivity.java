package com.acmenxd.mvp.base;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.acmenxd.retrofit.NetManager;
import com.acmenxd.retrofit.callback.NetCallback;
import com.acmenxd.retrofit.callback.NetSubscriber;
import com.acmenxd.mvp.R;
import com.acmenxd.mvp.net.IAllRequest;
import com.acmenxd.mvp.utils.EventBusUtils;
import com.acmenxd.mvp.utils.Utils;
import com.acmenxd.mvp.utils.net.IMonitorListener;
import com.acmenxd.mvp.utils.net.Monitor;
import com.acmenxd.mvp.utils.net.NetStatus;
import com.acmenxd.mvp.widget.frame.BaseActivityFragmentLayout;
import com.acmenxd.mvp.widget.frame.BasisView;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * @author AcmenXD
 * @version v1.0
 * @github https://github.com/AcmenXD
 * @date 2016/12/16 16:01
 * @detail Activity基类
 */
public abstract class BaseActivity extends AppCompatActivity implements IActivityFragment {
    protected final String TAG = this.getClass().getSimpleName();

    // 统一持有Subscription
    private CompositeSubscription mSubscription;
    // 统一管理Presenters
    private List<BasePresenter> mPresenters;
    // 存储子控件
    private SparseArray<View> mChildViews;
    // 布局容器
    private FrameLayout mContentLayout;
    private FrameLayout mLoadingLayout;
    private FrameLayout mErrorLayout;
    private BaseActivityFragmentLayout mOtherLayout;
    private Dialog mLoadingDialog;
    // 视图view
    private View mContentView;
    private View mLoadingView;
    private View mErrorView;
    // 网络状态监控
    IMonitorListener mNetListener = new IMonitorListener() {
        @Override
        public void onConnectionChange(NetStatus status) {
            onNetStatusChange(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 子类onCreate之前调用
        onCreateBefore(savedInstanceState);
        // 设置base视图
        super.setContentView(R.layout.activity_base);
        // 初始化容器
        mSubscription = getCompositeSubscription();
        mPresenters = new ArrayList<>();
        mChildViews = new SparseArray<>();
        // 获取布局容器
        mContentLayout = getView(R.id.activity_base_contentLayout);
        mLoadingLayout = getView(R.id.activity_base_loadingLayout);
        mErrorLayout = getView(R.id.activity_base_errorLayout);
        mOtherLayout = getView(R.id.activity_base_otherLayout);
        // 设置默认的加载视图
        setLoadingView(BasisView.getLoadingView(this));
        // 设置默认的错误视图
        setErrorView(BasisView.getErrorView(this));
        // 默认显示加载视图
        showContentView();
        // 将此Activity添加到ActivityStackManager中管理
        ActivityStackManager.INSTANCE.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 网络监控反注册
        Monitor.unRegistListener(mNetListener);
        // EventBus事件反注册
        EventBusUtils.unregister(this);
        //解绑 Subscriptions
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        //解绑 Presenters
        if (mPresenters != null && mPresenters.size() > 0) {
            for (int i = 0, len = mPresenters.size(); i < len; i++) {
                mPresenters.get(i).unSubscribe();
            }
            mPresenters.clear();
        }
        //移除 ChildViews
        mChildViews.clear();
        //关闭 Dialog
        hideLoadingDialog();
        // 将此Activity在ActivityStackManager中移除
        ActivityStackManager.INSTANCE.removeActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 网络监控注册
        Monitor.registListener(mNetListener);
        // EventBus事件注册
        EventBusUtils.register(this);
    }

    /**
     * EventBus默认添加的函数(子类无法重写,无需关心此函数)
     * * EventBus注册时,类中必须有@Subscribe注解的函数
     */
    @Subscribe
    public final void eventBusDefault(Object object) {
    }
    //------------------------------------子类可重写的函数

    /**
     * 子类onCreate之前调用,用来设置横竖屏等需要在setContentView之前做的操作
     */
    protected void onCreateBefore(Bundle savedInstanceState) {
    }

    /**
     * 网络状态变换调用
     */
    protected void onNetStatusChange(NetStatus pNetStatus) {
    }
    //------------------------------------子类可使用的工具函数 & 继承自IActivityFragment接口

    /**
     * 退出应用程序
     */
    @Override
    public final void exit() {
        ActivityStackManager.INSTANCE.exit();
    }

    /**
     * 添加Subscriptions
     */
    @Override
    public final void addSubscriptions(Subscription... pSubscriptions) {
        getCompositeSubscription().addAll(pSubscriptions);
    }

    /**
     * 添加Presenters
     */
    @Override
    public final void addPresenters(BasePresenter... pPresenters) {
        if (pPresenters != null && pPresenters.length > 0) {
            if (mPresenters == null) {
                mPresenters = new ArrayList<>();
            }
            for (int i = 0, len = pPresenters.length; i < len; i++) {
                mPresenters.add(pPresenters[i]);
            }
        }
    }

    /**
     * 获取CompositeSubscription实例
     */
    @Override
    public final CompositeSubscription getCompositeSubscription() {
        if (mSubscription == null) {
            mSubscription = new CompositeSubscription();
        }
        return mSubscription;
    }

    /**
     * 获取Intent中数据参数
     */
    @Override
    public final Bundle getBundle() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        return bundle;
    }

    /**
     * 为了统一起启动方式,所以暂作过时处理
     * * 尽量不要使用此函数启动Activity
     */
    @Deprecated
    @Override
    public final void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    /**
     * 为了统一起启动方式,所以暂作过时处理
     * * 尽量不要使用此函数启动Activity
     */
    @Deprecated
    @Override
    public final void startActivity(Intent intent, Bundle options) {
        super.startActivity(intent, options);
    }

    /**
     * 启动Activity
     */
    @Override
    public final void startActivity(Class cls) {
        Intent intent = new Intent(this, cls);
        super.startActivity(intent);
    }

    /**
     * 启动Activity
     */
    @Override
    public final void startActivity(Class cls, Bundle bundle) {
        Intent intent = new Intent(this, cls);
        intent.putExtras(bundle);
        super.startActivity(intent);
    }

    /**
     * 启动Activity
     */
    @Override
    public final void startActivity(Class cls, Bundle bundle, int flags) {
        Intent intent = new Intent(this, cls);
        intent.putExtras(bundle);
        intent.setFlags(flags);
        super.startActivity(intent);
    }

    /**
     * 获取IAllRequest实例
     * * 开放重写,满足不同需求
     */
    @Override
    public IAllRequest request() {
        return NetManager.INSTANCE.commonRequest(IAllRequest.class);
    }

    /**
     * 根据IRequest类获取Request实例
     */
    @Override
    public final <T> T request(Class<T> pIRequest) {
        return NetManager.INSTANCE.request(pIRequest);
    }

    /**
     * 创建新的Retrofit实例
     * 根据IRequest类获取Request实例
     */
    @Override
    public final <T> T newRequest(Class<T> pIRequest) {
        return NetManager.INSTANCE.newRequest(pIRequest);
    }

    /**
     * 创建新的Retrofit实例,并设置超时时间
     * 根据IRequest类获取Request实例
     */
    @Override
    public final <T> T newRequest(int connectTimeout, int readTimeout, int writeTimeout, Class<T> pIRequest) {
        return NetManager.INSTANCE.newRequest(connectTimeout, readTimeout, writeTimeout, pIRequest);
    }

    /**
     * 统一处理因异步导致的 Activity|Fragment销毁时发生NullPointerException问题
     *
     * @param pCallback Net请求回调
     * @param setting   数组下标 ->
     *                  0.是否显示LoadingDialog(默认false)
     *                  1.isCancelable(是否可以通过点击Back键取消)(默认true)
     *                  2.isCanceledOnTouchOutside(是否在点击Dialog外部时取消Dialog)(默认false)
     */
    @Override
    public final <T> Callback<T> newCallback(final NetCallback<T> pCallback, final boolean... setting) {
        showLoadingDialogBySetting(setting);
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (!mSubscription.isUnsubscribed()) {
                    pCallback.onResponse(call, response);
                }
                hideLoadingDialog();
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (!mSubscription.isUnsubscribed()) {
                    pCallback.onFailure(call, t);
                }
                hideLoadingDialog();
            }
        };
    }

    /**
     * 统一处理因异步导致的 Activity|Fragment销毁时发生NullPointerException问题
     *
     * @param pSubscriber Net请求回调
     * @param setting     数组下标 ->
     *                    0.是否显示LoadingDialog(默认false)
     *                    1.isCancelable(是否可以通过点击Back键取消)(默认true)
     *                    2.isCanceledOnTouchOutside(是否在点击Dialog外部时取消Dialog)(默认false)
     */
    @Override
    public final <T> Subscriber<T> newSubscriber(final NetSubscriber<T> pSubscriber, final boolean... setting) {
        showLoadingDialogBySetting(setting);
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                if (!mSubscription.isUnsubscribed()) {
                    pSubscriber.onCompleted();
                }
                hideLoadingDialog();
            }

            @Override
            public void onError(Throwable e) {
                if (!mSubscription.isUnsubscribed()) {
                    pSubscriber.onError(e);
                }
                hideLoadingDialog();
            }

            @Override
            public void onNext(T pT) {
                if (!mSubscription.isUnsubscribed()) {
                    pSubscriber.onNext(pT);
                }
            }
        };
    }

    /**
     * 根据setting,检查是否显示LoadingDialog
     *
     * @param setting 数组下标 ->
     *                0.是否显示LoadingDialog(默认false)
     *                1.isCancelable(是否可以通过点击Back键取消)(默认true)
     *                2.isCanceledOnTouchOutside(是否在点击Dialog外部时取消Dialog)(默认false)
     */
    @Override
    public final void showLoadingDialogBySetting(final boolean... setting) {
        boolean isShow = false;
        boolean isCancelable = true;
        boolean isCanceledOnTouchOutside = false;
        if (setting != null) {
            if (setting.length >= 1) {
                isShow = setting[0];
            }
            if (setting.length >= 2) {
                isCancelable = setting[1];
            }
            if (setting.length >= 3) {
                isCanceledOnTouchOutside = setting[2];
            }
        }
        if (isShow == true) {
            showLoadingDialog(isCancelable, isCanceledOnTouchOutside);
        }
    }

    /**
     * 显示LoadingDialog
     *
     * @param setting 数组下标 ->
     *                0.isCancelable(是否可以通过点击Back键取消)(默认true)
     *                1.isCanceledOnTouchOutside(是否在点击Dialog外部时取消Dialog)(默认false)
     */
    @Override
    public final void showLoadingDialog(final boolean... setting) {
        boolean isCancelable = true;
        boolean isCanceledOnTouchOutside = false;
        if (setting != null) {
            if (setting.length >= 1) {
                isCancelable = setting[0];
            }
            if (setting.length >= 2) {
                isCanceledOnTouchOutside = setting[1];
            }
        }
        if (mLoadingDialog == null) {
            mLoadingDialog = new Dialog(this);
        }
        mLoadingDialog.setContentView(BasisView.getDialogView(this));
        mLoadingDialog.setCancelable(isCancelable);
        mLoadingDialog.setCanceledOnTouchOutside(isCanceledOnTouchOutside);
        mLoadingDialog.show();
    }

    /**
     * 隐藏LoadingDialog
     */
    @Override
    public final void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }
    //------------------------------------ContentView|LoadingView|ErrorView相关操作 & 继承自IActivityFragment接口

    /**
     * 设置内容视图
     */
    @Override
    public final void setContentView(int layoutResId) {
        View view = LayoutInflater.from(this).inflate(layoutResId, null);
        setContentView(view);
    }

    @Override
    public final void setContentView(View view) {
        if (view == null) {
            return;
        }
        mContentView = view;
        mContentLayout.removeAllViews();
        mContentLayout.addView(mContentView);
    }

    /**
     * 设置加载视图
     */
    @Override
    public final void setLoadingView(View view) {
        if (view == null) {
            return;
        }
        mLoadingView = view;
        mLoadingLayout.removeAllViews();
        mLoadingLayout.addView(mLoadingView);
    }

    /**
     * 设置错误视图
     */
    @Override
    public final void setErrorView(View view) {
        if (view == null) {
            return;

        }
        mErrorView = view;
        mErrorLayout.removeAllViews();
        mErrorLayout.addView(mErrorView);
    }

    /**
     * 显示内容视图,隐藏其他视图
     */
    @Override
    public final void showContentView() {
        showContentView(false);
    }

    @Override
    public final void showContentView(boolean animat) {
        if (animat) {
            BasisView.layoutCancelInOutAnimation(this, mContentLayout, mContentLayout, mLoadingLayout, mErrorLayout);
        } else {
            BasisView.layouts$setVisibility(mContentLayout, mContentLayout, mLoadingLayout, mErrorLayout);
        }
    }

    /**
     * 显示加载视图,隐藏其他视图
     */
    @Override
    public final void showLoadingView() {
        showLoadingView(false);
    }

    @Override
    public final void showLoadingView(boolean animat) {
        if (animat) {
            BasisView.layoutCancelInOutAnimation(this, mLoadingLayout, mContentLayout, mLoadingLayout, mErrorLayout);
        } else {
            BasisView.layouts$setVisibility(mLoadingLayout, mContentLayout, mLoadingLayout, mErrorLayout);
        }
    }

    /**
     * 显示错误视图,隐藏其他视图
     */
    @Override
    public final void showErrorView() {
        showErrorView(false);
    }

    @Override
    public final void showErrorView(boolean animat) {
        if (animat) {
            BasisView.layoutCancelInOutAnimation(this, mErrorLayout, mContentLayout, mLoadingLayout, mErrorLayout);
        } else {
            BasisView.layouts$setVisibility(mErrorLayout, mContentLayout, mLoadingLayout, mErrorLayout);
        }
    }

    /**
     * 隐藏内容视图
     */
    @Override
    public final void hideContentView() {
        mContentLayout.setVisibility(View.GONE);
    }

    /**
     * 隐藏加载视图
     */
    @Override
    public final void hideLoadingView() {
        mLoadingLayout.setVisibility(View.GONE);
    }

    /**
     * 隐藏错误视图
     */
    @Override
    public final void hideErrorView() {
        mErrorLayout.setVisibility(View.GONE);
    }

    /**
     * 获取内容视图实例
     */
    @Override
    public final View getContentView() {
        return mContentView;
    }

    /**
     * 获取加载视图实例
     */
    @Override
    public final View getLoadingView() {
        return mLoadingView;
    }

    /**
     * 获取错误视图实例
     */
    @Override
    public final View getErrorView() {
        return mErrorView;
    }
    //------------------------------------子类可使用的工具函数 & 继承自IActivityFragment接口

    /**
     * 通过viewId获取控件
     */
    @Override
    public final <T extends View> T getView(int viewId) {
        View view = mChildViews.get(viewId);
        if (view == null) {
            view = this.findViewById(viewId);
            mChildViews.put(viewId, view);
        }
        return (T) view;
    }

    /**
     * 串拼接
     *
     * @param strs 可变参数类型
     * @return 拼接后的字符串
     */
    @Override
    public final String appendStrs(Object... strs) {
        return Utils.appendStrs(strs);
    }

    /**
     * 串变化 -> 大小&颜色
     *
     * @param start 从0开始计数(包含start)
     * @param end   从1开始计数(包含end)
     */
    @Override
    public final SpannableString changeStr(String str, int start, int end, int dip, int color) {
        return Utils.changeStr(str, start, end, dip, color);
    }

    @Override
    public final SpannableString changeStr(SpannableString spannableString, int start, int end, int dip, int color) {
        return Utils.changeStr(spannableString, start, end, dip, color);
    }

    /**
     * 根据手机的分辨率从 dp 的单位转成 px(像素)
     */
    @Override
    public final float dp2px(float dp) {
        return Utils.dp2px(this, dp);
    }

    /**
     * 根据手机的分辨率从 px(像素)的单位转成 dp
     */
    @Override
    public final float px2dp(float px) {
        return Utils.px2dp(this, px);
    }

    /**
     * 根据手机的分辨率从 sp 的单位转成 px(像素)
     */
    @Override
    public final float sp2px(float sp) {
        return Utils.sp2px(this, sp);
    }

    /**
     * 根据手机的分辨率从 px(像素)的单位转成 sp
     */
    @Override
    public final float px2sp(float px) {
        return Utils.px2sp(this, px);
    }

}
