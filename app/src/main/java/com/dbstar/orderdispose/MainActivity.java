package com.dbstar.orderdispose;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dbstar.orderdispose.adapter.DetailAdapter;
import com.dbstar.orderdispose.adapter.OrderAdapter;
import com.dbstar.orderdispose.adapter.WrapContentLinearLayoutManager;
import com.dbstar.orderdispose.bean.Order;
import com.dbstar.orderdispose.bean.OrderDetail;
import com.dbstar.orderdispose.constant.Constant;
import com.dbstar.orderdispose.constant.URL;
import com.dbstar.orderdispose.printer.PrinterConnectDialog;
import com.dbstar.orderdispose.service.AutoUpdateService;
import com.dbstar.orderdispose.service.OnMessageListener;
import com.dbstar.orderdispose.ui.SettingActivity;
import com.dbstar.orderdispose.utils.HttpUtil;
import com.google.gson.Gson;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.CustomerDisplay;
import com.gprinter.io.GpDevice;
import com.gprinter.io.GpEquipmentPort;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "dsw_MainActivity";

    private MyApplication application;
    private DrawerLayout mDrawerLayout;

    //对话框
    private Dialog mNewOrderDialog;
    //自动打印状态
    private Boolean auto_print_state = false;

    //Handler
    private Handler mHandler = new MyHandler();


    //区分订单列表类型
    private RadioButton main_rb_unhandlelist;//未处理订单查询
    private RadioButton main_rb_historylist;//历史订单查询
    private static final boolean UNHANDLELIST = false;
    private static final boolean HISTORYLIST = true;
    private Boolean flag_list = UNHANDLELIST;

    //订单数据
    private List<Order.OrderBean> datas = new ArrayList<Order.OrderBean>();
    private RecyclerView main_rv_orderlist;//订单列表recycle人view
    private OrderAdapter mMyAdapter;//订单列表adapter
    private SwipeRefreshLayout swipeRefreshLayout;//订单列表下拉刷新控件
    private RecyclerView mian_rv_detaillist;//订单详情列表recycle人view

    //订单详情数据
    private List<OrderDetail.OrderDetailBean> datasDetail = new ArrayList<OrderDetail.OrderDetailBean>();
    private TextView main_tv_detailcount;
    private TextView main_tv_detailmonney;
    private Button main_btn_print;
    private Button main_btn_ignore;
    private DetailAdapter detailAdapter;
    private OrderDetail orderDetail;
    private String orderTime;
    private String orderStutas;
    private String orderRoomId;
    private String orderStatus;
    private String orderNumber;
    private String orderType;

    //打印订单
    private PrinterServiceConnection conn = null;
    private GpService mGpService = null;
    private CustomerDisplay port;
    public static final String CONNECT_STATUS = "connect.status";
    //
    private int mPrinterIndex = 0;
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int REQUEST_PRINT_LABEL = 0xfd;
    private static final int REQUEST_PRINT_RECEIPT = 0xfc;
    private int mTotalCopies = 0;

    //是否处于正在轮询打印订单的状态：如果是在打印订单的状态中，service的update的方法就不要刷新列表了，因为轮询状态就会刷新列表
    //一旦能够刷新到新数据，准备打印，打印中，为true
    private Boolean isOrderPrinting = false;

    //自动更新服务连接对象
    private ServiceConnection conn_update = new AutoUpdateServiceConnection();
    private Button bt_update_dialog;
    private PortParameters mPortParameters;
    private Runnable mConnectToDevice;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        application = (MyApplication) getApplication();


        //侧滑菜单设置
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.nav_homepage);//菜单的点击事件
        navView.setNavigationItemSelectedListener(new MyOnNavigationItemSelectedListener());

        //Toolbar左侧的引导按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator((R.drawable.ic_menu));
        }


        //参考demo：GpSample(AndroidStudio)
        // 注册实时状态查询广播
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS));
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_RECEIPT_RESPONSE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_LABEL_RESPONSE));
//        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_DEVICE_STATUS));
        openPort();
        //绑定自动更新服务，获得打印服务对象
        connection();


        //RadioButton按钮监听
        main_rb_unhandlelist = (RadioButton) findViewById(R.id.main_rb_unhandlelist);
        main_rb_historylist = (RadioButton) findViewById(R.id.main_rb_historylist);
        //已处理订单按钮监听
        main_rb_unhandlelist.setOnClickListener(this);
        //历史订单选中监听
        main_rb_historylist.setOnClickListener(this);

        //订单列表：orderlist填充数据
        //datas.add();
        mMyAdapter = new OrderAdapter(this, datas);
        main_rv_orderlist = (RecyclerView) this.findViewById(R.id.main_rv_orderlist);
        main_rv_orderlist.setLayoutManager(new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        main_rv_orderlist.setAdapter(mMyAdapter);

        //订单详情列表：detaillist填充数据
        detailAdapter = new DetailAdapter(this, datasDetail);
        mian_rv_detaillist = (RecyclerView) this.findViewById(R.id.main_rv_detaillist);
        mian_rv_detaillist.setLayoutManager(new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mian_rv_detaillist.setAdapter(detailAdapter);
        main_tv_detailcount = (TextView) findViewById(R.id.main_tv_detailcount);
        main_tv_detailmonney = (TextView) findViewById(R.id.main_tv_detailmonney);
        main_btn_print = (Button) findViewById(R.id.main_btn_print);
        main_btn_print.setOnClickListener(this);
        main_btn_ignore = (Button) findViewById(R.id.main_btn_ignore);
        main_btn_ignore.setOnClickListener(this);
        //下拉刷新控件注册
        swipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.swipeLayout);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        swipeRefreshLayout.setOnRefreshListener(new OnOrderListSwipeRefreshListener());

        //启动后台数据更新服务
        init_dialog();
        Intent intent = new Intent(this, AutoUpdateService.class);
        bindService(intent, conn_update, Context.BIND_AUTO_CREATE);

        //开机自动连接打印机
        connect2print();
    }

    private void connect2print() {
        mPortParameters = new PortParameters();
        mPortParameters.setPortType(PortParameters.USB);
        mPortParameters.setIpAddr("192.168.123.100");
        mPortParameters.setPortNumber(9100);


        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // Get the list of attached devices
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = devices.values().iterator();
        int count = devices.size();
        Log.d(TAG, "count " + count);
        if (count > 0) {
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                String devicename = device.getDeviceName();
                Log.d(TAG, "devicename " + devicename);
                if (checkUsbDevicePidVid(device)) {
                    mPortParameters.setUsbDeviceName(devicename);
                }
            }
        }
        // 一个打印机的接口信息
        // mPortParameters
        mConnectToDevice = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run_UsbDeviceName " + mPortParameters.getUsbDeviceName());
                connectOrDisConnectToDevice();
            }
        };
        mHandler.postDelayed(mConnectToDevice, 2000);
    }


    boolean checkUsbDevicePidVid(UsbDevice dev) {
        int pid = dev.getProductId();
        int vid = dev.getVendorId();
        boolean rel = false;
        if ((vid == 34918 && pid == 256) || (vid == 1137 && pid == 85)
                || (vid == 6790 && pid == 30084)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 512)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 768)
                || (vid == 26728 && pid == 1024) || (vid == 26728 && pid == 1280)
                || (vid == 26728 && pid == 1536)) {
            rel = true;
        }
        return rel;
    }

    /**
     * 连接设备
     *
     * @param ，设备编号，设备列表 第 1 台设备
     */
    synchronized void connectOrDisConnectToDevice() {
        //连接第一台打印机
        int printerId = 0;
        int rel = 0;
        Log.e(TAG, String.valueOf(mPortParameters.getPortOpenState()));

        if (mPortParameters.getPortOpenState() == false) {
            //此时为断开情况，，拟连接
            if (CheckPortParamters(mPortParameters)) {
                try {
                    mGpService.closePort(printerId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                switch (mPortParameters.getPortType()) {
                    case PortParameters.USB:
                        try {

                            rel = mGpService.openPort(printerId, mPortParameters.getPortType(),
                                    mPortParameters.getUsbDeviceName(), 0);
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    case PortParameters.ETHERNET:
                        try {
                            rel = mGpService.openPort(printerId, mPortParameters.getPortType(),
                                    mPortParameters.getIpAddr(), mPortParameters.getPortNumber());
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    case PortParameters.BLUETOOTH:
                        try {
                            rel = mGpService.openPort(printerId, mPortParameters.getPortType(),
                                    mPortParameters.getBluetoothAddr(), 0);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
                Log.e(TAG, "result :" + String.valueOf(r));
                if (r != GpCom.ERROR_CODE.SUCCESS) {
                    if (r == GpCom.ERROR_CODE.DEVICE_ALREADY_OPEN) {
                        mPortParameters.setPortOpenState(true);
                    } else {
                        messageBox(GpCom.getErrorText(r));
                    }
                }
            } else {
                messageBox(getString(R.string.port_parameters_wrong));
            }
        } else {
            //此时为连接情况，拟断开
//            Log.d(TAG, "DisconnectToDevice ");
//            try {
//                mGpService.closePort(0);
//            } catch (RemoteException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }
    }

    Boolean CheckPortParamters(PortParameters param) {
        boolean rel = false;
        int type = param.getPortType();
        if (type == PortParameters.BLUETOOTH) {
            if (!param.getBluetoothAddr().equals("")) {
                rel = true;
            }
        } else if (type == PortParameters.ETHERNET) {
            if ((!param.getIpAddr().equals("")) && (param.getPortNumber() != 0)) {
                rel = true;
            }
        } else if (type == PortParameters.USB) {
            if (!param.getUsbDeviceName().equals("")) {
                rel = true;
            }
        }
        return rel;
    }

    private void messageBox(String err) {
        Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //访问网络，刷新订单列表
        getUnHandleOrderList();
        mHandler.postDelayed(mConnectToDevice, 1000);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            //case R.id.backup:
            //    Toast.makeText(this, "You clicked Backup", Toast.LENGTH_SHORT).show();
            //    break;
            default:
        }
        return true;
    }


    //暴露handler对象
    public Handler getHandler() {
        return mHandler;
    }

    //初始化对话框
    private void init_dialog() {
        mNewOrderDialog = new Dialog(this, R.style.my_dialog);
        LinearLayout root = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.update_dialog, null);
        bt_update_dialog = (Button) root.findViewById(R.id.bt_update_dialog);
        bt_update_dialog.setOnClickListener(this);
        mNewOrderDialog.setContentView(root);
        Window dialogWindow = mNewOrderDialog.getWindow();
        dialogWindow.setGravity(Gravity.BOTTOM);
        dialogWindow.setWindowAnimations(R.style.dialogstyle); // 添加动画
        WindowManager.LayoutParams lp = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        lp.x = 0; // 新位置X坐标
        lp.y = -20; // 新位置Y坐标
        lp.width = (int) getResources().getDisplayMetrics().widthPixels; // 宽度
//      lp.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度
//      lp.alpha = 9f; // 透明度
        root.measure(0, 0);
        lp.height = root.getMeasuredHeight();
        lp.alpha = 9f; // 透明度
        dialogWindow.setAttributes(lp);
        //mNewOrderDialog.show();
    }


    //跳转到设置页面
    private void openSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }


    //根据订单号，获取详情数据
    public void getDetailList(String orderStatus, String seqnumber, final Boolean isPrintOnGet) {
        String urlOrderDetail ;
        if ("0".equals(orderStatus) || "2".equals(orderStatus)) {
            orderType = "Shoping";
            urlOrderDetail = application.getServiceIP() + URL.OrderItem + "?" + URL.NUMBER + "=" + seqnumber;
        }else {
            orderType = "Movie";
            urlOrderDetail = application.getServiceIP() + "/bar/media/getMediaOrderItem.do?ordersId=" + seqnumber;
        }
        try {
            HttpUtil.sendOkHttpRequest(urlOrderDetail, new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    //订单详情数据 json
                    orderDetail = null;
                    try {
                        orderDetail = new Gson().fromJson(json, OrderDetail.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    datasDetail.clear();

                    if (orderDetail == null) {
                        //刷新详情列表
                        mHandler.sendEmptyMessage(4);
                        return;
                    }

                    datasDetail.addAll(orderDetail.getData());
                    mHandler.sendEmptyMessage(4);

                    //打印订单列表第一条
                    if (isPrintOnGet && application.isPrintAuto()) {
                        //设置为打印状态？
                        isOrderPrinting = true;
                        //打印并标记datas_0
                        printOrderDetail();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //刷新详情列表
    public void refreshDetailList() {
        detailAdapter.notifyDataSetChanged();
        // notifyDataSetChanged() 是异步方法？小计会不会优先于详情列表刷新出来？
        if (orderDetail != null) {
            main_tv_detailcount.setText("" + orderDetail.getTotalCount());//总数量
            main_tv_detailmonney.setText("" + orderDetail.getTotalMoney());//总金额
            //设置电话号码
            if (Constant.ORDER_TYPE_SHOPING.equals(orderDetail.getOrderType())) {
                //
            } else if (Constant.ORDER_TYPE_MEAL.equals(orderDetail.getOrderType())) {
                //
            }
        } else {
            main_tv_detailcount.setText("0");//总数量
            main_tv_detailmonney.setText("0");//总金额
        }

    }


    //获取未处理电影订单列表
    private void getUnHandleMoviesOrderList() {
        try {
            HttpUtil.sendOkHttpRequest(application.getServiceIP() + "/bar/media/getMediaNewOrder.do", new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    //解析访问网络获取到的 json数据 ，打印出来
                    Order order = null;
                    try {
                        order = new Gson().fromJson(json, Order.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "未处理电影订单: " + order);

                    if (order != null) {
                        datas.addAll(order.getData());
                    }

                    Collections.sort(datas, new Comparator() {
                        public int compare(Object a, Object b) {
                            String pre = ((Order.OrderBean) a).getCreatedate();
                            String next = ((Order.OrderBean) b).getCreatedate();
                            return next.compareTo(pre);
                        }
                    });

                    // 设置 全局最后一次访问网络获取的 订单数目
                    application.setOrderListSize(datas.size());

                    // 再次确定类型
                    flag_list = UNHANDLELIST;
                    // 刷新订单列表
                    mHandler.sendEmptyMessage(2);


                    //如果设置了自动打印，把第一条打印出来
                    if (application.isPrintAuto()) {
                        Order.OrderBean datas_0 = null;
                        if (datas != null && !datas.isEmpty()) {
                            datas_0 = datas.get(0);
                            //根据订单号，访问网络，刷新详情列表
                        }
                        //无论什么情况，获取到订单列表，把第一条订单抓取出来，显示在详情列表中
                        if (datas_0 != null) {
                            String seqs = datas_0.getNumber();
                            //访问订单详情并打印出来
                            //打印前给房间号、订单号赋值
                            orderNumber = seqs;
                            orderRoomId = datas_0.getRoomId();
                            orderTime = datas_0.getCreatedate();
                            orderStutas = datas_0.getStatus();
                            getDetailList(orderStutas, seqs, true);
                        } else {
                            isOrderPrinting = false;
                        }
                    } else {
                        //通知主线程，刷新详情列表，置空详情列表
                        orderDetail = null;
                        datasDetail.clear();
                        mHandler.sendEmptyMessage(4);
                    }

                }

                @Override
                public void onFailure(Call call, IOException e) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取未处理点餐订单列表
    public void getUnHandleOrderList() {

        datas.clear();

        try {
            HttpUtil.sendOkHttpRequest(application.getServiceIP() + URL.NewOrder, new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    Log.d(TAG, "未处理订单: " + json);

                    //解析访问网络获取到的 json数据 ，打印出来
                    Order order = null;
                    try {
                        order = new Gson().fromJson(json, Order.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "未处理点餐订单: " + order);


                    // 通知主线程，刷新订单列表
                    if (order != null) {
                        datas.addAll(order.getData());
                    }
                    // 获取未处理电影订单列表
                    getUnHandleMoviesOrderList();
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    // 获取未处理电影订单列表
                    getUnHandleMoviesOrderList();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 已处理电影订单列表
    private void getHandleMoviesOrderList() {
        try {
            HttpUtil.sendOkHttpRequest(application.getServiceIP() + "/bar/media/getMediaOldOrder.do", new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    //解析访问网络获取到的 json数据 ，打印出来
                    Order order = null;
                    try {
                        order = new Gson().fromJson(json, Order.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "已处理电影订单: " + order);

                    if (order != null) {
                        datas.addAll(order.getData());
                    }

                    Collections.sort(datas, new Comparator() {
                        public int compare(Object a, Object b) {
                            String pre = ((Order.OrderBean) a).getCreatedate();
                            String next = ((Order.OrderBean) b).getCreatedate();
                            return next.compareTo(pre);
                        }
                    });

                    mHandler.sendEmptyMessage(2);

                    //通知主线程，刷新详情列表，置空详情列表
                    orderDetail = null;
                    datasDetail.clear();
                    mHandler.sendEmptyMessage(4);

                }

                @Override
                public void onFailure(Call call, IOException e) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取 历史订单列表
    public void getHistoryOrderList() {
        datas.clear();
        try {
            HttpUtil.sendOkHttpRequest(application.getServiceIP() + URL.OldOrder, new Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();

                    //解析访问网络获取到的 json数据 ，打印出来
                    Order order = null;
                    try {
                        order = new Gson().fromJson(json, Order.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "未处理订单: " + order);

                    if (order != null) {
                        datas.addAll(order.getData());
                    }

                    getHandleMoviesOrderList();
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    getHandleMoviesOrderList();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 标记订单处理完毕
     *
     * @param seqnumber ,订单编号
     */
    public void markOrderCompled(String seqnumber) {
        String markUrk;
        if("Shoping".equals(orderType)) {
            markUrk = application.getServiceIP() + URL.OrderMark + "?" + URL.OrderMarkID + "=" + seqnumber + "&" + URL.OrderMarkFLAG;
        }else if("Movie".equals(orderType)){
            markUrk = application.getServiceIP() + "/bar/media/getMediaOldOrderStaus.do?ordersId=" + seqnumber;
        }else {
            markUrk = null;
        }

        try {
            HttpUtil.sendOkHttpRequest(markUrk, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    //循环中移除未处理订单列表的 orderNumber 订单

                    //处理订单之后，再进行查询，刷新一次列表
                    getUnHandleOrderList();

                    //刷新完 订单列表 ，刷新详情列表
                    //未处理订单列表刷新后，详情列表应该清空
                    //清空、刷新详情列表
                    //设置详情页javabean 为空

                    orderDetail = null;
                    datasDetail.clear();
                    mHandler.sendEmptyMessage(4);

                }

                @Override
                public void onFailure(Call call, IOException e) {
                    //
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_update_dialog: // 对话框点击事件:刷新列表
                if (mNewOrderDialog != null) {
                    mNewOrderDialog.dismiss();
                }
                getUnHandleOrderList();
                flag_list = UNHANDLELIST;
                break;
            case R.id.main_rb_unhandlelist:
                //访问网络 查询 未处理订单，更新订单列表
                getUnHandleOrderList();
                flag_list = UNHANDLELIST;
                break;
            case R.id.main_rb_historylist:
                //访问网络 查询 历史订单，更新订单列表
                getHistoryOrderList();
                flag_list = HISTORYLIST;
                break;
            case R.id.main_btn_ignore:
                if (orderDetail == null) {
                    break;
                }
                //如果目前是已处理订单，无需标记订单为已处理，直接退出
                if (flag_list == HISTORYLIST) {
                    break;
                }
                //访问后台，标记订单已处理
                markOrderCompled(orderNumber);
                break;
            case R.id.main_btn_print:
                Log.d(TAG, "onClick: main_btn_print.");
                printOrderDetail();
                break;
            default:
                break;
        }
    }

    //打印订单详情
    public void printOrderDetail() {
        if (orderDetail == null || mGpService == null) {
            return;
        }
        //打印小票
        //如果小票打印机未连接，提示完 退出
        //目前测试结果，默认情况下只有打印机设置列表第0个打印机连接才能打印
        try {
            if (!(mGpService.getPrinterConnectStatus(0) == GpDevice.STATE_CONNECTED)) {
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //打印订单
        for (int i = 0; i < application.getPrint_count(); i++) {
            printTicket();
        }
        //如果目前是已处理订单，打印完不需要标记订单已处理，直接退出
        if (flag_list == HISTORYLIST) {
            return;
        }
        //访问后台，标记订单已处理
        markOrderCompled(orderNumber);
    }

    //打印小票
    private void printTicket() {

        //数据准备
        //房间号、订单号、创建时间
        //orderTime,orderRoomId,orderNumber;
        //订单详情
        //datasDetail;

        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 1);
//        esc.addText("一二三四五六七八九十一二三四五六\n");
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设 置 打 印 居 中
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设 置 为 倍 高 倍 宽
        esc.addText("房间：" + orderRoomId + "\n");// 打 印 文 字
        //进纸一行
//        esc.addPrintAndLineFeed();

        /* 打 印 文 字 */
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取 消 倍 高 倍 宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设 置 打 印 左 对 齐
        esc.addText("订单号：" + orderNumber + "\n");// 打 印 文 字
//        esc.addText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n");// 打 印 文 字

        esc.addPrintAndLineFeed();
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        esc.addText("名称");
        esc.addSetHorAndVerMotionUnits((byte) 6, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("单价");
        //可能的效果是，“数量”往后移动了一个单位——也就是“数量”这两个字符的占位
        esc.addSetRelativePrintPositon((short) 1);
        esc.addText("数量");
        esc.addText("\n---------------------------\n");


        for (OrderDetail.OrderDetailBean bean : datasDetail) {
            esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
            esc.addText(bean.getGoodsName());
            esc.addSetHorAndVerMotionUnits((byte) 6, (byte) 0);
            esc.addSetAbsolutePrintPosition((short) 6);
            esc.addText(bean.getPrice() + " 元");
            //可能的效果是，“数量”往后移动了一个单位——也就是“数量”这两个字符的占位
            esc.addSetRelativePrintPositon((short) 1);
            esc.addText(bean.getBuynum() + " 份");
            esc.addText("\n");
        }

        esc.addText("\n---------------------------\n");
        esc.addPrintAndLineFeed();
        esc.addText("小计：" + orderDetail.getTotalMoney() + " 元\n");
        esc.addPrintAndLineFeed();
        esc.addText("时间：" + orderTime + "\n");
        esc.addPrintAndLineFeed();
        if (Constant.ORDER_TYPE_SHOPING.equals(orderDetail.getOrderType())) {
            esc.addText("购物电话：" + Constant.PHONE_NUMBER_SHOPING + "\n");
        } else if (Constant.ORDER_TYPE_MEAL.equals(orderDetail.getOrderType())) {
            esc.addText("订餐电话：" + Constant.PHONE_NUMBER_MEAL + "\n");
        }

        esc.addPrintAndLineFeed();
        esc.addText("客户签名：\n");


        // 开 钱 箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);

        //打印并且走纸多少行
        esc.addPrintAndFeedLines((byte) 7);
        //方法说明:获得打印命令
        Vector<Byte> datas = esc.getCommand(); // 发 送 数 据

        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;

        try {
            rs = mGpService.sendEscCommand(mPrinterIndex, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            //Auto - generated catch block
            e.printStackTrace();
        }
    }


private class OnOrderListSwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
    @Override
    public void onRefresh() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (flag_list) {
                    getHistoryOrderList();
                } else {
                    getUnHandleOrderList();
                }
                mHandler.sendEmptyMessage(1);
            }
        }).start();
    }
}


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        if (conn != null) {
            unbindService(conn); // unBindService
        }
        if (conn_update != null) {
            unbindService(conn_update);
        }
        unregisterReceiver(mBroadcastReceiver);
    }

private class MyHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case 1:
                //手动下拉 刷新 订单列表
                //setRefreshing(false) 停止刷新动画
                swipeRefreshLayout.setRefreshing(false);
                //mMyAdapter.notifyDataSetChanged();
                //swipeRefreshLayout.setEnabled(false);
                break;
            case 2:
                //点击 radio button ,刷新 订单列表
                if (flag_list == UNHANDLELIST) {
                    main_rb_unhandlelist.setChecked(true);
                }
                mMyAdapter.setFlag(false);
                mMyAdapter.notifyDataSetChanged();
                break;
            case 3:
                //adapter中，选中订单列表 某订单，发送消息，访问网络，获取详情列表 javabean
                Bundle bundle = msg.getData();
                orderTime = bundle.getString("orderTime");
                orderRoomId = bundle.getString("orderRoomId");
                orderNumber = bundle.getString("orderNumber");
                orderStatus = bundle.getString("orderStatus");
                getDetailList(orderStatus, orderNumber, false);
                break;
            case 4:
                //刷新详情列表
                refreshDetailList();
                break;
            default:
                break;
        }
    }
}

private class AutoUpdateServiceConnection implements ServiceConnection {
    private AutoUpdateService myService;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        myService = ((AutoUpdateService.AutoUpdateServiceBinder) service).getService();
        myService.setOnMessageListener(new OnMessageListener() {
            @Override
            public void onUpdate(final int isUpdate) {
                Log.d("Service", "onUpdate");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isUpdate == Constant.MSG_NET_ERR) {
                            if (mNewOrderDialog != null && !mNewOrderDialog.isShowing()) {
                                bt_update_dialog.setText("网络连接断开，请检查网络");
                                mNewOrderDialog.show();
                            }
                            return;
                        }
                        if (isUpdate == Constant.MSG_NET_OK) {
                            if (mNewOrderDialog != null && mNewOrderDialog.isShowing()) {
                                mNewOrderDialog.dismiss();
                            }
                            return;
                        }

                        if (!application.isPrintAuto() && mNewOrderDialog != null && !mNewOrderDialog.isShowing()) {
                            //设置为不自动打印，显示新订单对话框
                            bt_update_dialog.setText("您有新订单，请及时处理");
                            mNewOrderDialog.show();
                        } else if (application.isPrintAuto() && isOrderPrinting == false) {
                            //打印新订单
                            //1、访问网络，获取新的订单
                            getUnHandleOrderList();
                            //获取新订单后，就判断是否打印新订单，是自动打印新订单，就打印
                            flag_list = UNHANDLELIST;
                            main_rb_unhandlelist.setChecked(true);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}


private class MyOnNavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_homepage:
                mDrawerLayout.closeDrawers();
                break;
            case R.id.nav_conncet:
                //跳转到 打印服务连接界面
                openPortDialogueClicked();
                mDrawerLayout.closeDrawers();
                break;
            case R.id.nav_settings:
                //跳转到 打印服务连接界面
                openSettings();
                mDrawerLayout.closeDrawers();
                break;
            default:
                break;
        }
        return true;
    }
}


/*****************************************************************************************************************************/
/*****************************************************************************************************************************/

/**
 * 打印相关代码
 */
class PrinterServiceConnection implements ServiceConnection {
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i("ServiceConnection", "onServiceDisconnected() called");
        mGpService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mGpService = GpService.Stub.asInterface(service);
    }
}

    private BroadcastReceiver mBroadcastReceiver = new PrintAboutBroadcastReceiver();

private class PrintAboutBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("TAG", action);
        // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
        if (action.equals(GpCom.ACTION_DEVICE_REAL_STATUS)) {

            // 业务逻辑的请求码，对应哪里查询做什么操作
            int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
            // 判断请求码，是则进行业务操作
            if (requestCode == MAIN_QUERY_PRINTER_STATUS) {

                int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                String str;
                if (status == GpCom.STATE_NO_ERR) {
                    str = "打印机正常";
                } else {
                    str = "打印机 ";
                    if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {
                        str += "脱机";
                    }
                    if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                        str += "缺纸";
                    }
                    if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                        str += "打印机开盖";
                    }
                    if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                        str += "打印机出错";
                    }
                    if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {
                        str += "查询超时";
                    }
                }

                Toast.makeText(getApplicationContext(), "打印机：" + mPrinterIndex + " 状态：" + str, Toast.LENGTH_SHORT)
                        .show();
            } else if (requestCode == REQUEST_PRINT_LABEL) {
                int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                if (status == GpCom.STATE_NO_ERR) {
                    sendLabel();
                } else {
                    Toast.makeText(MainActivity.this, "query printer status error", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_PRINT_RECEIPT) {
                int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                if (status == GpCom.STATE_NO_ERR) {
                    sendReceipt();
                } else {
                    Toast.makeText(MainActivity.this, "query printer status error", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (action.equals(GpCom.ACTION_RECEIPT_RESPONSE)) {
            if (--mTotalCopies > 0) {
                sendReceiptWithResponse();
            }
        } else if (action.equals(GpCom.ACTION_LABEL_RESPONSE)) {
            byte[] data = intent.getByteArrayExtra(GpCom.EXTRA_PRINTER_LABEL_RESPONSE);
            int cnt = intent.getIntExtra(GpCom.EXTRA_PRINTER_LABEL_RESPONSE_CNT, 1);
            String d = new String(data, 0, cnt);
            /**
             * 这里的d的内容根据RESPONSE_MODE去判断返回的内容去判断是否成功，具体可以查看标签编程手册SET
             * RESPONSE指令
             * 该sample中实现的是发一张就返回一次,这里返回的是{00,00001}。这里的对应{Status,######,ID}
             * 所以我们需要取出STATUS
             */
            Log.d("LABEL RESPONSE", d);

            if (--mTotalCopies > 0 && d.charAt(1) == 0x00) {
                sendLabelWithResponse();
            }
        }
//            else if(action.equals(GpCom.ACTION_DEVICE_STATUS)){
//                //打印完成广播
//            }
    }

}

    void sendReceipt() {

        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addText("Sample\n"); // 打印文字
        esc.addPrintAndLineFeed();

		/* 打印文字 */
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
        esc.addText("Print text\n"); // 打印文字
        esc.addText("Welcome to use SMARNET printer!\n"); // 打印文字

		/* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        // esc.addText(message,"BIG5");
        esc.addText(message, "GB2312");
        esc.addPrintAndLineFeed();

		/* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("智汇");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("网络");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("设备");
        esc.addPrintAndLineFeed();

		/* 打印图片 */
        esc.addText("Print bitmap!\n"); // 打印文字
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.gprinter);
        esc.addRastBitImage(b, 384, 0); // 打印图片

		/* 打印一维条码 */
        esc.addText("Print code128\n"); // 打印文字
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);//
        // 设置条码可识别字符位置在条码下方
        esc.addSetBarcodeHeight((byte) 60); // 设置条码高度为60点
        esc.addSetBarcodeWidth((byte) 1); // 设置条码单元宽度为1
        esc.addCODE128(esc.genCodeB("SMARNET")); // 打印Code128码
        esc.addPrintAndLineFeed();

		/*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
		 */
        esc.addText("Print QRcode\n"); // 打印文字
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
        esc.addSelectSizeOfModuleForQRCode((byte) 3);// 设置qrcode模块大小
        esc.addStoreQRCodeData("www.smarnet.cc");// 设置qrcode内容
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

		/* 打印文字 */
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("Completed!\r\n"); // 打印结束
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = mGpService.sendEscCommand(mPrinterIndex, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void sendReceiptWithResponse() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addText("Sample\n"); // 打印文字
        esc.addPrintAndLineFeed();

		/* 打印文字 */
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
        esc.addText("Print text\n"); // 打印文字
        esc.addText("Welcome to use SMARNET printer!\n"); // 打印文字

		/* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        // esc.addText(message,"BIG5");
        esc.addText(message, "GB2312");
        esc.addPrintAndLineFeed();

		/* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("智汇");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("网络");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("设备");
        esc.addPrintAndLineFeed();

		/* 打印图片 */
        // esc.addText("Print bitmap!\n"); // 打印文字
        // Bitmap b = BitmapFactory.decodeResource(getResources(),
        // R.drawable.gprinter);
        // esc.addRastBitImage(b, 384, 0); // 打印图片

		/* 打印一维条码 */
        esc.addText("Print code128\n"); // 打印文字
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);//
        // 设置条码可识别字符位置在条码下方
        esc.addSetBarcodeHeight((byte) 60); // 设置条码高度为60点
        esc.addSetBarcodeWidth((byte) 1); // 设置条码单元宽度为1
        esc.addCODE128(esc.genCodeB("SMARNET")); // 打印Code128码
        esc.addPrintAndLineFeed();

		/*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
		 */
        esc.addText("Print QRcode\n"); // 打印文字
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
        esc.addSelectSizeOfModuleForQRCode((byte) 3);// 设置qrcode模块大小
        esc.addStoreQRCodeData("www.smarnet.cc");// 设置qrcode内容
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

		/* 打印文字 */
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("Completed!\r\n"); // 打印结束
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);

        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播
        esc.addQueryPrinterStatus();

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = mGpService.sendEscCommand(mPrinterIndex, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void sendLabel() {
        LabelCommand tsc = new LabelCommand();
        tsc.addSize(60, 60); // 设置标签尺寸，按照实际尺寸设置
        tsc.addGap(0); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
        tsc.addCls();// 清除打印缓冲区
        // 绘制简体中文
        tsc.addText(20, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "Welcome to use SMARNET printer!");
        // 绘制图片
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.gprinter);
        tsc.addBitmap(20, 50, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);

        tsc.addQRCode(250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc");
        // 绘制一维条码
        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET");
        tsc.addPrint(1, 1); // 打印标签
        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendLabelCommand(mPrinterIndex, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void sendLabelWithResponse() {
        LabelCommand tsc = new LabelCommand();
        tsc.addSize(60, 60); // 设置标签尺寸，按照实际尺寸设置
        tsc.addGap(0); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
        tsc.addCls();// 清除打印缓冲区
        // 绘制简体中文
        tsc.addText(20, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "Welcome to use SMARNET printer!");
        // 绘制图片
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.gprinter);
        tsc.addBitmap(20, 50, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);

        tsc.addQRCode(250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc");
        // 绘制一维条码
        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET");
        tsc.addPrint(1, 1); // 打印标签
        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);

        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendLabelCommand(mPrinterIndex, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void connection() {
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
    }

    private void openPort() {
        port = CustomerDisplay.getInstance(this);
        try {
            // 打开端口
            port.openPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 设置监听回调数据
        port.setReceivedListener(new GpEquipmentPort.OnDataReceived() {
            /**
             * 获取客显屏的状态开启或关闭
             *
             * @param isOpen
             *            客显屏背光灯开启或关闭
             */
            @Override
            public void onPortOpen(boolean isOpen) {
                if (isOpen) {
                    toast("打开端口成功");
                } else {
                    toast("打开端口失败");
                }

            }

            /**
             * 获取客显屏背光灯开启或关闭
             *
             * @param isOn
             *            客显屏背光灯开启或关闭
             */
            @Override
            public void onBacklightStatus(final boolean isOn) {
                Log.d("==onBacklightStatus==", String.valueOf(isOn));

                toast("==onBacklightStatus== 背光灯状态->" + String.valueOf(isOn));
            }

            /**
             * 获取客显屏光标的位置
             *
             * @param x
             *            横坐标
             * @param y
             *            纵坐标
             */
            @Override
            public void onCursorPosition(final int x, final int y) {
                toast("==onCursorPosition==x = " + x + ",y =" + y);
                Log.d("==onCursorPosition==", "x坐标 = " + x + ",y坐标 =" + y);
            }

            /**
             * 获取客显屏的行和列
             *
             * @param row
             *            行
             * @param column
             *            列
             */
            @Override
            public void onDisplayRowAndColumn(final int row, final int column) {
                toast("行数 = " + row + ",列数 =" + column);
                Log.d("==onCursorPosition==", "row = " + row + ",column =" + column);
            }

            /**
             * 获取客显屏背光灯超时时间
             *
             * @param timeout
             *            单位：秒
             */
            @Override
            public void onBacklightTimeout(final int timeout) {
                toast("超时时间 = " + timeout);
                Log.d("==onBacklightTimeout==", "timeout = " + timeout);
            }

            /**
             * 更新客显固件完成回调方法
             */
            @Override
            public void onUpdateSuccess() {
//		if (mProgressDialog != null) {
//			mProgressDialog.dismiss();
//		}
//		Toast.makeText(this, "更新完成", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUpdateFail(String error) {
//		if (mProgressDialog != null) {
//			mProgressDialog.dismiss();
//		}
//		Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        port.getBacklightTimeout();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public boolean[] getConnectState() {
        boolean[] state = new boolean[GpPrintService.MAX_PRINTER_CNT];
        for (int i = 0; i < GpPrintService.MAX_PRINTER_CNT; i++) {
            state[i] = false;
        }
        for (int i = 0; i < GpPrintService.MAX_PRINTER_CNT; i++) {
            try {
                if (mGpService.getPrinterConnectStatus(i) == GpDevice.STATE_CONNECTED) {
                    state[i] = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    //打开打印机连接对话框
    public void openPortDialogueClicked() {
        if (mGpService == null) {
            Toast.makeText(this, "Print Service is not start, please check it", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, PrinterConnectDialog.class);
        boolean[] state = getConnectState();
        intent.putExtra(CONNECT_STATUS, state);
        this.startActivity(intent);
    }

    /**
     * 打印相关代码
     */
    /*****************************************************************************************************************************/
    /*****************************************************************************************************************************/

}