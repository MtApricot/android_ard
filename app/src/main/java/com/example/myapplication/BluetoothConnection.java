package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

public class BluetoothConnection {

    private static final String TAG = "BluetoothConnection";

    Context context;

    //Bluetooth SPP（Serial Port Profile）用UUID
    private static final UUID SERIAL_PORT_PROFILE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothDevice mDevice;
    private BluetoothSocket mSocket = null;

    private ConnectThread mConnectThread;

    //Bluetooth接続が確立・切断された通知を受けるクラスが実装するインタフェース
    public interface connectionInterface{
        //Bluetooth接続の確立通知を受けるメソッド
        void onBluetoothConnected();
        // Bluetooth接続失敗の通知を受けるメソッド
        void onBluetoothConnectFailed();
    }

    private connectionInterface connectionListener = null;
    //リスナを設定するメソッド
    public void setConnectionListener(connectionInterface listener) {
        connectionListener = listener;
    }
    //Bluetoothデバイスのオブジェクトの取得
    public BluetoothConnection(Context _context, String bluetoothAddress) {
        this(_context, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bluetoothAddress));
    }

    //Bluetoothデバイスの情報の取得
    public BluetoothConnection(Context _context, BluetoothDevice device) {
        context = _context;
        mDevice = Objects.requireNonNull(device);
    }

    //@Override
    //Bluetoothデバイスのアドレスの取得
    public String getAddress() {
        return mDevice.getAddress();
    }

    public void connect() {
        //スレッドを開始して指定されたデバイスに接続する
        mConnectThread = new ConnectThread();
        mConnectThread.start();
    }

    //    @Override
    //ソケットの接続状態を返す
    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }
    //接続を解放する
    public synchronized void close() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (isConnected()) {
            try {
                mSocket.close();
                Log.d(TAG, "mSocket.close");
            } catch (IOException e) {
                Log.w(TAG, "IOException during BluetoothSocket close(): " + e);
            } finally {
                mSocket = null;
            }
        } else {
            Log.d(TAG, "is NOT Connected");
        }
    }

    //    @Override
    //Bluetoothデバイスとのデータ通信用の入力ストリームを取得する
    public InputStream getInputStream() {
        if (isConnected()) {
            try {
                return mSocket.getInputStream();
            } catch (IOException e) {
                Log.w(TAG, "failed to get Bluetooth input stream: " + e);
            }
        }
        return null;
    }

    //    @Override
    //Bluetoothデバイスとのデータ通信用の出力ストリームを取得する
    public OutputStream getOutputStream() {
        if (isConnected()) {
            try {
                return mSocket.getOutputStream();
            } catch (IOException e) {
                Log.w(TAG, "failed to get Bluetooth output stream: " + e);
            }
        }
        return null;
    }


     //デバイスと接続

    private class ConnectThread extends Thread {

        public ConnectThread() {

            // 指定されたBluetoothDeviceとの接続用のBluetoothSocketを取得する
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mSocket = mDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE);
            } catch (IOException e) {
                Log.w(TAG, "BluetoothConnection couldn't be established due to an exception: " + e);
                mSocket = null;
            }
        }

        public void run() {
            Log.d("BEGIN ConnectThread", "Thread name = " + Thread.currentThread().getName());

            //BluetoothSocketに接続する

            try {
                //ブロッキング呼び出し、接続が成功した場合または例外が発生した場合にのみ返す
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mSocket.connect();
            } catch (IOException e) {
                //ソケットを閉じる
                try {
                    mSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }
                Log.d(TAG, "connectionFailed", e);
                //Bluetooth接続が失敗した場合、コネクションリスナーに通知する
                if(connectionListener != null){
                    connectionListener.onBluetoothConnectFailed();
                }
                return;
            }

            //完了、ConnectThread をリセットする
            synchronized (BluetoothConnection.this) {
                Log.d(TAG, "mConnectThread = null");
                mConnectThread = null;
            }

            Log.d(TAG, "Connected !");
            //Bluetooth接続が成功した場合、コネクションリスナーに通知する
            if(connectionListener != null){
                connectionListener.onBluetoothConnected();
            }
        }
        //通信をキャンセルする
        public void cancel() {
            try {
                Log.d(TAG, "mSocket.close()");
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "socket failed", e);
            }
        }
    }
}
