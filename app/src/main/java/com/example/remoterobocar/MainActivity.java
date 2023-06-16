package com.example.remoterobocar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
//import android.os.CountDownTimer;
import android.os.Message;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DEV_APP";

    private Spinner list_device;//danh sách thiết bị quét được
    private Button btn_scan;//nút quét
    private Button btn_connect;//nút kết nối
    private SeekBar seekbar1;// điều chỉnh tốc độ xe
    private SeekBar seekbar2;// điều chỉnh độ nhạy rẽ hướng
    private Button btn_up;
    private Button btn_down;
    private Button btn_left;
    private Button btn_right;
    private TextView tv_btn_connect;
    private TextView tv_btn_scan;
    private TextView txt_sp1, txt_sp2;
    private SwitchCompat swt;
    //    EditText data_send;
    //    Button btn_send;

    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothSocket BTSocket = null;
    private Set<BluetoothDevice> BTPairedDevices = null;
    classBTInitDataCommunication cBTInitSendReceive = null;
    private ArrayList<String> ID;
    private String BtAddress = null;
    private BluetoothDevice BTDevice = null;
    private ArrayAdapter<String> BTArrayAdapter;
    private ArrayList<BluetoothDevice> discoveredDevices; // thiết bị mới đã quét được

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //biến kiểm tra
    private boolean bBTConnected; // check connected
    private boolean btn_up_pressed;
    private boolean btn_down_pressed;
    private boolean btn_right_pressed;
    private boolean btn_left_pressed;
    private boolean isSendingCommand = false;// check dữ liệu có đang truyền đi không
    private String Command = "";//dữ liệu truyền
    private String lastCommand = "";//dữ liệu cuối cùng được truyền

    static public final int REQUEST_ENABLE_LOCATION = 1;
    static public final int REQUEST_ENABLE_BT = 1;
    static public final int REQUEST_ACCESS_COARSE_LOCATION = 1;

    static public final int BT_CON_STATUS_NOT_CONNECTED = 0;
    static public final int BT_CON_STATUS_CONNECTING = 1;
    static public final int BT_CON_STATUS_CONNECTED = 2;
    static public final int BT_CON_STATUS_FAILED = 3;
    static public final int BT_CON_STATUS_CONNECTiON_LOST = 4;
    static public int iBTConnectionStatus = BT_CON_STATUS_NOT_CONNECTED;

    static final int BT_STATE_LISTENING = 1;
    static final int BT_STATE_CONNECTING = 2;
    static final int BT_STATE_CONNECTED = 3;
    static final int BT_STATE_CONNECTION_FAILED = 4;
    static final int BT_STATE_MESSAGE_RECEIVED = 5;

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "getAction ");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); //lấy thiết bị tìm được
                if (device != null) {
                    String name = device.getName();
                    String address = device.getAddress();
                    if (name != null && !discoveredDevices.contains(device)) { //nếu tên khác null , và chưa có trong danh sách
                        discoveredDevices.add(device);
                        BTArrayAdapter.add(name);
                        BTArrayAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Add : " + name);
                        Toast.makeText(MainActivity.this, "Tìm thấy thiết bị", Toast.LENGTH_SHORT).show();
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                tv_btn_scan.setText("Scan");
                Toast.makeText(MainActivity.this, "Scanning finished", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                tv_btn_scan.setText("Scanning");

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(MainActivity.this, "kết nối thành công", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Log.d(TAG, "onCreate-Start");

        list_device = findViewById(R.id.list_device);
        btn_scan = findViewById(R.id.btn_scan);
        btn_connect = findViewById(R.id.btn_connect);
        seekbar1 = findViewById(R.id.speed1);
        seekbar2 = findViewById(R.id.speed2);
        btn_up = findViewById(R.id.btn_up);
        btn_down = findViewById(R.id.btn_down);
        btn_left = findViewById(R.id.btn_left);
        btn_right = findViewById(R.id.btn_right);
        txt_sp1 = findViewById(R.id.txt_speed1);
        txt_sp2 = findViewById(R.id.txt_speed2);
        //        message = findViewById(R.id.massage_send);
        tv_btn_connect = findViewById(R.id.tv_connect);
        tv_btn_scan = findViewById(R.id.tv_scan);
        swt = findViewById(R.id.switch1);
        //        message.setMovementMethod(new ScrollingMovementMethod());
        //        message.setText("App loader");

        ID = new ArrayList<String>();
        discoveredDevices = new ArrayList<BluetoothDevice>();
        BTArrayAdapter = new ArrayAdapter<String>(this, R.layout.style_list_spinner, ID);
        BTArrayAdapter.setDropDownViewResource(R.layout.style_list_spinner);
        list_device.setAdapter(BTArrayAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReceiver, filter);


        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanDevice();
            }
        });

        //                data_send = findViewById(R.id.data_send);
        //                btn_send = findViewById(R.id.btn_send);


        list_device.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                BluetoothDevice device = discoveredDevices.get(position); // Lấy vị trí trong list)
                BtAddress = device.getAddress(); // gán địa chỉ của thiết bị được chọn vào biến để kết nối
                Toast.makeText(getApplicationContext(), "You selected " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                BtAddress = null;
            }
        });

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button Click buttonBTConnect");
                if (!bBTConnected) {
                    if (BtAddress == null) {
                        Log.d(TAG, "Please select BT device");
                        Toast.makeText(getApplicationContext(), "Please select Bluetooth Device", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "SelectedItemPosition = 0");
                        return;
                    }
                    String sSelectedDevice = list_device.getSelectedItem().toString();
                    Log.d(TAG, "Selected device = " + sSelectedDevice);
                    //                    for (BluetoothDevice BTDev : discoveredDevices){
                    //                        if (sSelectedDevice.equals(BTDev.getName())){
                    //
                    //                        }
                    //                    }
                    BTDevice = myBluetoothAdapter.getRemoteDevice(BtAddress);
                    myBluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "Selected device UUID = " + BtAddress);
                    cBluetoothConnect cBTConnect = new cBluetoothConnect(BTDevice);
                    cBTConnect.start();

                    //                    for (BluetoothDevice BTDev : BTPairedDevices) {
                    //                        if (sSelectedDevice.equals(BTDev.getName())) {
                    //                            BTDevice = BTDev;
                    //                            Log.d(TAG, "Selected device UUID = " + BTDevice.getAddress());
                    //
                    //                            cBluetoothConnect cBTConnect = new cBluetoothConnect(BTDevice);
                    //                            cBTConnect.start();
                    //
                    //                        }
                    //                    }
                } else {
                    Log.d(TAG, "Disconnecting BTConnection");
                    if (BTSocket != null && BTSocket.isConnected()) {
                        try {
                            BTSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "BTDisconnect Exp " + e.getMessage());
                        }
                    }
                    tv_btn_connect.setText("Connect");
                    bBTConnected = false;
                }
            }
        });

        swt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (swt.isChecked()) {
                    sendMessage("mode,1");
                    btn_up.setEnabled(false);
                    btn_down.setEnabled(false);
                    btn_right.setEnabled(false);
                    btn_left.setEnabled(false);
                } else {
                    sendMessage("mode,0");
                    btn_up.setEnabled(true);
                    btn_down.setEnabled(true);
                    btn_right.setEnabled(true);
                    btn_left.setEnabled(true);
                }
            }
        });

        seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
                int i2 = Math.round((float) i / 255 * 100);
                txt_sp1.setText(MessageFormat.format("Speed1: {0}%", i2));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (progress <= 50) {
                    Toast.makeText(getApplicationContext(), "Tốc độ quá thấp có thể không chạy được", Toast.LENGTH_SHORT).show();
                }
                sendMessage("speed1," + progress);
            }
        });

        seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
                int i2 = Math.round((float) i / 255 * 100);
                txt_sp2.setText(MessageFormat.format("Speed2: {0}%", i2));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (progress <= 50) {
                    Toast.makeText(getApplicationContext(), "Độ nhạy quá thấp", Toast.LENGTH_SHORT).show();
                }
                sendMessage("speed2," + progress);
            }
        });

        //                btn_send.setOnClickListener(new View.OnClickListener() {
        //                    @Override
        //                    public void onClick(View view) {
        //                        String smessage = data_send.getText().toString();
        //        //                message.append("\n->" + smessage);
        //                        sendMessage(smessage);
        //                    }
        //                });

        // F : Tiến
        // B : Lùi
        // L : Trái
        // R : Phải
        btn_up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!bBTConnected) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btn_up_pressed = true;
                        btn_up.setBackgroundResource(R.drawable.ic_button_up_pr);
                        // Người dùng đã nhấn nút, bắt đầu gửi lệnh điều khiển
                        if (btn_right_pressed) {
                            Command = "FR";
                            //                            startSendata("FR");
                        } else if (btn_left_pressed) {
                            //                            startSendata("FL");
                            Command = "FL";
                        } else {
                            //                            startSendata("F");
                            Command = "F";
                        }
                        startSendata(Command);
                        return true;
                    case MotionEvent.ACTION_UP:
                        btn_up_pressed = false;
                        btn_up.setBackgroundResource(R.drawable.ic_button_up);
                        // Người dùng đã thả nút, kết thúc gửi lệnh điều khiển
                        if (Command.equals("FL")) {
                            Command = "L";
                        } else if (Command.equals("FR")) {
                            Command = "R";
                        }
                        startSendata(Command);
                        stopSenđata("F");
                        return true;
                }
                return false;
            }
        });

        btn_down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!bBTConnected) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btn_down_pressed = true;
                        btn_down.setBackgroundResource(R.drawable.ic_button_down_pr);
                        if (btn_right_pressed) {
                            Command = "BR";
                        } else if (btn_left_pressed) {
                            Command = "BL";
                        } else {
                            Command = "B";
                        }
                        startSendata(Command);
                        return true;
                    case MotionEvent.ACTION_UP:
                        btn_down.setBackgroundResource(R.drawable.ic_button_down);
                        btn_down_pressed = false;
                        if (Command.equals("BL")) {
                            Command = "L";
                        } else if (Command.equals("BR")) {
                            Command = "R";
                        }
                        startSendata(Command);
                        stopSenđata("B");
                        return true;
                }
                return false;
            }
        });

        btn_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!bBTConnected) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btn_left_pressed = true;
                        btn_left.setBackgroundResource(R.drawable.ic_button_left_pr);
                        if (btn_up_pressed) {
                            //                            startSendata("FL");
                            Command = "FL";
                        } else if (btn_down_pressed) {
                            Command = "BL";
                        } else {
                            //                            startSendata("L");
                            Command = "L";
                        }
                        startSendata(Command);
                        return true;
                    case MotionEvent.ACTION_UP:
                        btn_left_pressed = false;
                        btn_left.setBackgroundResource(R.drawable.ic_button_left);
                        if (Command.equals("FL")) {
                            Command = "F";
                            startSendata(Command);
                        } else if (Command.equals("BL")) {
                            Command = "B";
                            startSendata(Command);
                        }
                        stopSenđata("L");
                        return true;
                }
                return false;
            }
        });

        btn_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!bBTConnected) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btn_right_pressed = true;
                        btn_right.setBackgroundResource(R.drawable.ic_button_right_pr);
                        if (btn_up_pressed) {
                            //                        startSendata("FR");
                            Command = "FR";
                        } else if (btn_down_pressed) {
                            Command = "BR";
                        } else {
                            //                            startSendata("R");
                            Command = "R";
                        }
                        startSendata(Command);
                        return true;
                    case MotionEvent.ACTION_UP:
                        btn_right_pressed = false;
                        btn_right.setBackgroundResource(R.drawable.ic_button_right);
                        if (Command.equals("FR")) {
                            Command = "F";
                            startSendata(Command);
                        } else if (Command.equals("BR")) {
                            Command = "B";
                            startSendata(Command);
                        }
                        stopSenđata("R");
                        return true;
                }
                return false;
            }
        });
    }

    public void startSendata(String c) { //bắt đầu gửi dữ liệu liên tục
        //        sendMessage(c);
        isSendingCommand = true;
        lastCommand = c;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isSendingCommand && lastCommand.equals(c)) {
                    sendMessage(c);
                    try {
                        Thread.sleep(60);// 60ms gửi 1 lần
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stopSenđata(String c) {
        //        sendMessage(c);
        if (!btn_up_pressed && !btn_down_pressed && !btn_left_pressed && !btn_right_pressed) {
            isSendingCommand = false;
        }
    }

    public void SetBluetooth() {
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Kiểm tra và yêu cầu quyền truy cập Bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, REQUEST_ENABLE_BT);
        }
        // Kiểm tra và yêu cầu quyền truy cập địa chỉ MAC
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_LOCATION);
        }
        if (!myBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_LONG).show();
        }
    }

    //Quét các thiết bị bluetooth xung quanh
    public void ScanDevice() {
        Log.d(TAG, "ScanDevice");

        if (myBluetoothAdapter.isDiscovering()) {
            myBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "cancelDiscovery");
        } else {
            BTArrayAdapter.clear();
            BTPairedDevices = myBluetoothAdapter.getBondedDevices();
            Log.d(TAG, "getBTPairedDevices , Paired devices count = " + BTPairedDevices.size());
            for (BluetoothDevice BTDev : BTPairedDevices) {
                Log.d(TAG, BTDev.getName() + ", " + BTDev.getAddress());
                discoveredDevices.add(BTDev);
                BTArrayAdapter.add(BTDev.getName());
                BTArrayAdapter.notifyDataSetChanged();
            }
            myBluetoothAdapter.startDiscovery();
            Log.d(TAG, "startDiscovery....");
            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public class cBluetoothConnect extends Thread {
        public cBluetoothConnect(BluetoothDevice BTDevice) {
            Log.i(TAG, "classBTConnect-start");
            try {
                BTSocket = BTDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception exp) {
                Log.e(TAG, "classBTConnect-exp" + exp.getMessage());
            }
        }

        public void run() {
            try {
                BTSocket.connect();
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTED;
                handler.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    public class classBTInitDataCommunication extends Thread {
        private InputStream inputStream = null; // Dữ liệu nhận được từ thiết bị Bluetooth
        private OutputStream outputStream = null; // Dữ liệu gửi đi đến thiết bị Bluetooth

        public classBTInitDataCommunication(BluetoothSocket socket) {
            Log.i(TAG, "classBTInitDataCommunication-start");

            try {
                inputStream = socket.getInputStream(); // Lấy dữ liệu nhận được từ BluetoothSocket
                outputStream = socket.getOutputStream(); // Lấy dữ liệu gửi đi từ BluetoothSocket
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "classBTInitDataCommunication-start, exp " + e.getMessage());
            }
        }

        public void run() {
            byte[] buffer = new byte[1024]; // Bộ đệm để lưu trữ dữ liệu nhận được
            int bytes; // Số byte đã nhận được từ InputStream

            // Vòng lặp vô hạn để đọc dữ liệu từ InputStream và gửi nó đến Handler
            while (BTSocket.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);// Đọc dữ liệu vào buffer
                    handler.obtainMessage(BT_STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();// Gửi dữ liệu đến Handler
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "BT disconnect from decide end, exp " + e.getMessage());
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTiON_LOST;
                    // Nếu xảy ra lỗi khi đọc dữ liệu, đóng kết nối Bluetooth
                    try {
                        //disconnect bluetooth
                        Log.d(TAG, "Disconnecting BTConnection");
                        if (BTSocket != null && BTSocket.isConnected()) {
                            BTSocket.close();
                        }
                        tv_btn_connect.setText("Connect");
                        bBTConnected = false;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        // Phương thức để ghi dữ liệu vào OutputStream để gửi đi đến thiết bị Bluetooth
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case BT_STATE_LISTENING:
                    Log.d(TAG, "BT_STATE_LISTENING");
                    break;
                case BT_STATE_CONNECTING:
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTING;
                    tv_btn_connect.setText("Connecting..");
                    Log.d(TAG, "BT_STATE_CONNECTING");
                    break;
                case BT_STATE_CONNECTED:
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTED;
                    Log.d(TAG, "BT_CON_STATUS_CONNECTED");
                    tv_btn_connect.setText("Disconnect");

                    cBTInitSendReceive = new classBTInitDataCommunication(BTSocket);
                    cBTInitSendReceive.start();

                    bBTConnected = true;
                    break;
                case BT_STATE_CONNECTION_FAILED:
                    iBTConnectionStatus = BT_CON_STATUS_FAILED;
                    Log.d(TAG, "BT_STATE_CONNECTION_FAILED");
                    bBTConnected = false;
                    break;

                case BT_STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    Log.d(TAG, "Message receive ( " + tempMsg.length() + " )  data : " + tempMsg);

                    //                    message.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    public void sendMessage(String sMessage) {
        if (BTSocket != null && iBTConnectionStatus == BT_CON_STATUS_CONNECTED) {
            if (BTSocket.isConnected()) {
                try {
                    sMessage += "#";//kí tự đánh dấu kết thúc một chuỗi được gửi đi
                    cBTInitSendReceive.write(sMessage.getBytes());
                    //                    message.append("\r\n-> " + sMessage);
                } catch (Exception exp) {

                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please connect to bluetooth", Toast.LENGTH_SHORT).show();
            //            message.append("\r\n Not connected to bluetooth");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume-Resume");
        SetBluetooth();
        //        getBTPairedDevices();
        //        populateSpinnerDevices();
        list_device.setAdapter(BTArrayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy-Start");
        unregisterReceiver(bReceiver);
        //        myBluetoothAdapter.disable();
    }
}

//    private void getBTPairedDevices() {
//        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (myBluetoothAdapter == null) {
//            data_send.setText("No Bluetooth Device in the phone");
//            return;
//        } else if (!myBluetoothAdapter.isEnabled()) {
//            data_send.setText("Please turn on Bluetooth");
//            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(turnOnIntent, 1);
////            return;
//        }
//        BTPairedDevices = myBluetoothAdapter.getBondedDevices();
//        Log.d(TAG, "getBTPairedDevices , Paired devices count = " + BTPairedDevices.size());
//        for (BluetoothDevice BTDev : BTPairedDevices) {
//            Log.d(TAG, BTDev.getName() + ", " + BTDev.getAddress());
//        }
//
//    }

//    void populateSpinnerDevices() {
//        ArrayList<String> alPairedDevices = new ArrayList<>();
//        alPairedDevices.add("Select");
//        for (BluetoothDevice BTDev : BTPairedDevices) {
//            alPairedDevices.add(BTDev.getName());
//        }
//        final ArrayAdapter<String> aaPairedDevices = new ArrayAdapter<>(this, android.support.constraint.R.layout.support_simple_spinner_dropdown_item, alPairedDevices);
//        aaPairedDevices.setDropDownViewResource(android.support.constraint.R.layout.support_simple_spinner_dropdown_item);
//        list_device.setAdapter(aaPairedDevices);
//    }

//Cách nhấn khác
//        btn_up.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        buttonPressed = true;
//                        new CountDownTimer(Long.MAX_VALUE, 200) { // Chạy liên tục sau mỗi giây
//                            @Override
//                            public void onTick(long millisUntilFinished) {
//                                if (!buttonPressed) {
//                                    cancel();
//                                } else {
//                                    sendMessage("f");
//                                }
//
//                            }
//
//                            @Override
//                            public void onFinish() {}
//                        }.start();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        buttonPressed = false;
//                        return true;
//                }
//                return false;
//            }
//        });

//        img_title.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view) {
//                if (chophepgui){
//                    if (light_status) { writeData("0");}
//                    else writeData("1");
//                    beginListenForData();
//                }else{
//                    Toast.makeText(MainActivity.this, "chưa gửi được", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });