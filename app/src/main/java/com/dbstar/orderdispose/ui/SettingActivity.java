package com.dbstar.orderdispose.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dbstar.orderdispose.MainActivity;
import com.dbstar.orderdispose.MyApplication;
import com.dbstar.orderdispose.R;
import com.dbstar.orderdispose.constant.Constant;
import com.dbstar.orderdispose.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wh on 2017/1/6.
 */
public class SettingActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private DrawerLayout mDrawerLayout;
    private ToggleButton mSet_tb_print;
    private SharedPreferences sp;
    private SharedPreferences.Editor sp_editor;
    private ToggleButton set_tb_voice;
    private TextView set_tv_count;
    private EditText set_et_ip;
    private Button set_bt_ipset;
    private int print_count = 1;    //打印次数
    private MyApplication application;
    private List<String> typeList;
    private ArrayAdapter<String> adapter;
    private Spinner mSet_sp_type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        application = (MyApplication) getApplication();

        Toolbar toolbar = (Toolbar) findViewById(R.id.set_toolbar);
        setSupportActionBar(toolbar);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.set_drawer_layout);

        //侧滑菜单设置
        NavigationView navView = (NavigationView) findViewById(R.id.set_nav_view);
        navView.setCheckedItem(R.id.set_nav_homepage);//菜单的点击事件
        navView.setNavigationItemSelectedListener(new MyOnNavigationItemSelectedListener());

        //Toolbar左侧的引导按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator((R.drawable.ic_arrow_back));

        }

        sp = this.getSharedPreferences("config", MODE_PRIVATE);
        sp_editor = sp.edit();

        //订单类型
        mSet_sp_type = (Spinner) findViewById(R.id.set_sp_type);
        typeList = new ArrayList<String>();
        typeList.add("电影点播订单");
        typeList.add("客房送物订单");
        typeList.add("点餐购物订单");
        typeList.add("点餐购物送物");

        initSpinner();

        //新订单自动打印
        boolean isPrintAuto = sp.getBoolean(Constant.AUTO_PRINT,false);
        mSet_tb_print = (ToggleButton) findViewById(R.id.set_tb_print);
        mSet_tb_print.setChecked(isPrintAuto);
        mSet_tb_print.setOnCheckedChangeListener(this);

        //新订单语音提示
        boolean isVoiceEnable = sp.getBoolean(Constant.VOICE_ENABLE,false);
        set_tb_voice = (ToggleButton) findViewById(R.id.set_tb_voice);
        set_tb_voice.setChecked(isVoiceEnable);
        set_tb_voice.setOnCheckedChangeListener(this);

        //订单打印重复次数
        Button set_bt_count_up = (Button)findViewById(R.id.set_bt_count_up);
        Button set_bt_count_down = (Button)findViewById(R.id.set_bt_count_down);
        set_bt_count_up.setOnClickListener(this);
        set_bt_count_down.setOnClickListener(this);
        set_tv_count = (TextView)findViewById(R.id.set_tv_count);
        print_count = sp.getInt(Constant.PRINT_COUNT, 1);
        set_tv_count.setText(""+ print_count);

        //服务地址设置
        set_et_ip = (EditText)findViewById(R.id.set_et_ip);
        set_bt_ipset = (Button) findViewById(R.id.set_bt_ipset);
        set_bt_ipset.setOnClickListener(this);

        //保存并返回
        Button set_bt_back = (Button)findViewById(R.id.set_bt_back);
        set_bt_back.setOnClickListener(this);



    }

    private void initSpinner() {
        adapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item, typeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSet_sp_type.setAdapter(adapter);
        // 设置默认值
        setSpinnerDefaultType();
        mSet_sp_type.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
		        // atg2 是 typeList 的顺序
                switch (arg2){
                    case 0:
                        application.setOrdersType(Constant.ORDER_TYPE_FILM);
                        sp_editor.putString(Constant.ORDERS_TYPE,application.getOrdersType());
                        break;
                    case 1:
                        application.setOrdersType(Constant.ORDER_TYPE_SERVICE);
                        sp_editor.putString(Constant.ORDERS_TYPE,application.getOrdersType());
                        break;
                    case 2:
                        application.setOrdersType(Constant.ORDER_TYPE_SHOPPING);
                        sp_editor.putString(Constant.ORDERS_TYPE,application.getOrdersType());
                        break;
                    case 3:
                        application.setOrdersType(Constant.ORDER_TYPE_MEAL_SHOPING_SERVICE);
                        sp_editor.putString(Constant.ORDERS_TYPE,application.getOrdersType());
                        break;
                }
		        /* 将mSet_sp_type 显示*/
                arg0.setVisibility(View.VISIBLE);
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
                arg0.setVisibility(View.VISIBLE);
            }
        });
        /*下拉菜单弹出的内容选项触屏事件处理*/
        mSet_sp_type.setOnTouchListener(new Spinner.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        /*下拉菜单弹出的内容选项焦点改变事件处理*/
        mSet_sp_type.setOnFocusChangeListener(new Spinner.OnFocusChangeListener(){
            public void onFocusChange(View v, boolean hasFocus) {
                // TODO Auto-generated method stub

            }
        });
    }

    private void setSpinnerDefaultType() {
        switch (application.getOrdersType()){
            case Constant.ORDER_TYPE_FILM:
                mSet_sp_type.setSelection(0,true);
                break;
            case Constant.ORDER_TYPE_SERVICE:
                mSet_sp_type.setSelection(1,true);
                break;
            case Constant.ORDER_TYPE_SHOPPING:
                mSet_sp_type.setSelection(2,true);
                break;
            case Constant.ORDER_TYPE_MEAL_SHOPING_SERVICE:
                mSet_sp_type.setSelection(3,true);
                break;
        }
    }


    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //引导按钮监听
            case android.R.id.home:
                onBackPressed();
                break;
            default:
        }
        return true;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            //引导按钮监听
            case R.id.set_tb_print:
                Log.d("CompoundButton", "onCheckedChanged: set_tb_print");
                sp_editor.putBoolean(Constant.AUTO_PRINT, isChecked);
                sp_editor.commit();
                application.setIsPrintAuto(isChecked);
                break;
            case R.id.set_tb_voice:
                Log.d("CompoundButton", "onCheckedChanged: set_tb_voice");
                sp_editor.putBoolean(Constant.VOICE_ENABLE, isChecked);
                sp_editor.commit();
                application.setIsVoiceEnable(isChecked);
                break;
            default:
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //引导按钮监听
            case R.id.set_bt_back:
                //保存打印次数
                sp_editor.putString(Constant.ORDERS_TYPE,application.getOrdersType());
                sp_editor.putInt(Constant.PRINT_COUNT,print_count);
                sp_editor.commit();
                application.setPrint_count(print_count);
                //返回
                onBackPressed();
                break;
            case R.id.set_bt_count_up:
                if ( print_count < Constant.PRINT_MAX_COUNT ) {
                    print_count += 1;

                }
                set_tv_count.setText("" + print_count);
                break;
            case R.id.set_bt_count_down:
                if( print_count > 1 ){
                    print_count -= 1;
                }
                set_tv_count.setText(""+print_count);
                break;
            case R.id.set_bt_ipset:
                serviceIpSet();
                break;
            default:
        }
    }

    private void serviceIpSet() {
        String service = "";
        if(set_et_ip.hasFocus()){
            //有焦点，说明在编辑状态，收集参数，保存，显示为只读
            set_bt_ipset.setText("查看");
            service = set_et_ip.getText().toString();

            //对IP地址进行判断，确定是IP地址进行保存，否则toast提示
            if(!isIP(service)){
                ToastUtils.showSafeToast(this,"IP地址输入错误，请检查");
                return;
            }

            sp_editor.putString(Constant.SERVICE_IP,service);
            sp_editor.commit();
            application.setServiceIP(service);

            set_et_ip.setText("服务器地址配置");
            set_et_ip.setFocusable(false);
            set_et_ip.setFocusableInTouchMode(false);
        }else{
            //无焦点，说明在只读状态，点击后设置为可编辑状态
            set_bt_ipset.setText("保存");
            service = sp.getString(Constant.SERVICE_IP,"");
            if("".equals(service)){
                set_et_ip.setText("请输入服务器IP");
            }else{
                set_et_ip.setText(service);
            }
            set_et_ip.setFocusableInTouchMode(true);
            set_et_ip.setFocusable(true);
            set_et_ip.requestFocus();
        }
    }

    public boolean isIP(String addr)
    {
        if(addr==null){
            return false;
        }
        if(addr.length() < 7 || addr.length() > 15 || "".equals(addr))
        {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                +"(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                +"(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                +"(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        return ipAddress;
    }


    private class MyOnNavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            switch (item.getItemId()){
                case R.id.set_nav_homepage:
                    mDrawerLayout.closeDrawers();
                    openMainActivity();
                    break;
                case R.id.set_nav_settings:
                    //跳转到 连接打印服务 界面
                    mDrawerLayout.closeDrawers();
                    break;
                default:break;
            }
            return true;
        }
    }
}
