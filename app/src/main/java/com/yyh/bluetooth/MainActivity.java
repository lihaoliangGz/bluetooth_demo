package com.yyh.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private Button btnSet;
    private Button btnPaired;
    private Button btnSearch;
    private Button btnStopSearch;
    private Button btnDiscoverable;
    private Button btnConnect;

    //低功耗蓝牙
    private Button btnLe;
    private Button btnEnable;
    private Button btnScanBle;

    /**
     * 蓝牙是否激活
     */
    private boolean enable = false;
    /**
     * 低功率蓝牙是否扫描中
     */
    private boolean BleScanning = false;
    /**
     * 是否支持低功率蓝牙
     */
    private boolean isSupportBle = false;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter BleAdapter;

    private ScanCallback scanCallback=new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i("yyh", "onScanResult: 执行");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i("yyh", "onBatchScanResults: 执行");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i("yyh", "onScanFailed: 执行");
        }
    };



    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("yyh", "onReceive: action="+action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("yyh", "Name=" + device.getName() + "，address=" + device.getAddress()+
                "，Uuid="+device.getUuids());
                if ("魅蓝 E2".equals(device.getName())) {
                    bluetoothDevice = device;
                }
            }

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                String str=intent.getParcelableExtra(BluetoothAdapter.EXTRA_SCAN_MODE);
                Log.i("yyh", "onReceive: str=" + str);
                String str2 = intent.getParcelableExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE);
                Log.i("yyh", "onReceive: str2=" + str2);
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
        setListener();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    /**
     * 初始化控件
     */
    private void assignViews() {
        btnSet = findViewById(R.id.btnSet);
        btnPaired = findViewById(R.id.btnPaired);
        btnSearch = findViewById(R.id.btnSearch);
        btnStopSearch = findViewById(R.id.btnStopSearch);
        btnDiscoverable = findViewById(R.id.btnDiscoverable);
        btnConnect = findViewById(R.id.btnConnect);

        btnLe = findViewById(R.id.btnLe);
        btnEnable = findViewById(R.id.btnEnable);
        btnScanBle = findViewById(R.id.btnScanBle);

    }

    private void setListener() {
        btnSet.setOnClickListener(this);
        btnPaired.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnStopSearch.setOnClickListener(this);
        btnDiscoverable.setOnClickListener(this);
        btnConnect.setOnClickListener(this);

        btnLe.setOnClickListener(this);
        btnEnable.setOnClickListener(this);
        btnScanBle.setOnClickListener(this);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //注册广播
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSet:
                setBluetooth();
                break;
            case R.id.btnPaired:
                paired();
                break;
            case R.id.btnSearch:
                boolean discovery = bluetoothAdapter.startDiscovery();
                if (discovery) {
                    Toast.makeText(this, "搜索中...", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnStopSearch:
                boolean stop = bluetoothAdapter.cancelDiscovery();
                if (stop) {
                    Toast.makeText(this, "已停止搜索", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnDiscoverable:
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(intent, 124);
                break;

            case R.id.btnConnect:
                new ConnectThread(bluetoothDevice).start();
                break;

            case R.id.btnLe:
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    isSupportBle = true;
                    Toast.makeText(this, "支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
                } else {
                    isSupportBle = false;
                    Toast.makeText(this, "不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.btnEnable:
                BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BleAdapter = bluetoothManager.getAdapter();
                if (BleAdapter != null && !BleAdapter.isEnabled()) {
                    Intent intentBle = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentBle, 125);
                } else if (BleAdapter != null && BleAdapter.isEnabled()) {
                    enable = true;
                    Toast.makeText(this, "已经激活了,不需要再激活", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnScanBle:
                if (enable) {
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            BleScanning = false;
//                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//                                BluetoothLeScanner bluetoothLeScanner = BleAdapter.getBluetoothLeScanner();
//                                bluetoothLeScanner.stopScan(scanCallback);
//                            }
//                        }
//                    }, 1000);

                    BleScanning = true;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        Log.i("yyh", "onClick: =====");
                        BluetoothLeScanner bluetoothLeScanner = BleAdapter.getBluetoothLeScanner();
                        bluetoothLeScanner.startScan(scanCallback);
                    }
                } else {
                    Toast.makeText(this, "请激活蓝牙", Toast.LENGTH_SHORT).show();
                }
                break;
            default:

                break;
        }
    }

    /**
     * 设置蓝牙
     */
    private void setBluetooth() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                //蓝牙未开启
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 123);
            } else {
                Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "本设备不支持蓝牙", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 查询已配对设备
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void paired() {
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {
                Log.i("yyh", "Name=" + device.getName() + "，Address=" + device.getAddress() +
                        "，Type=" + device.getType() + "，Uuids=" + (device.getUuids() == null ? null : device.getUuids().toString()));
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice device;
        private final BluetoothSocket socket;

        private ConnectThread(BluetoothDevice device) {
            this.device = device;
            ParcelUuid[] uuids = this.device.getUuids();
            BluetoothSocket tmp = null;
            Log.i("yyh", "ConnectThread: uuid="+uuids[0].getUuid());
//            try {
//                tmp = device.createRfcommSocketToServiceRecord();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            this.socket = tmp;
        }

        @Override
        public void run() {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.i("yyh", "run: isDiscovering");
            } else {
                Log.i("yyh", "run: not discovery");
            } 
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }


        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 123:
                if (RESULT_OK == resultCode) {
                    enable = true;
                    Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                } else if (RESULT_CANCELED == resultCode) {
                    enable = false;
                    Toast.makeText(this, "用户取消开启蓝牙", Toast.LENGTH_SHORT).show();
                }
                break;

            case 124:
                if (RESULT_OK == resultCode) {
                    Toast.makeText(this, "可检测性已开启", Toast.LENGTH_SHORT).show();
                } else if (RESULT_CANCELED == resultCode) {
                    Toast.makeText(this, "用户取消启用可检测性", Toast.LENGTH_SHORT).show();

                }
                break;

            case 125:
                if (RESULT_OK == resultCode) {
                    enable = true;
                    Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                } else if (RESULT_CANCELED == resultCode) {
                    enable = false;
                    Toast.makeText(this, "取消开启蓝牙", Toast.LENGTH_SHORT).show();
                }
                break;
            default:

                break;
        }
    }
}
