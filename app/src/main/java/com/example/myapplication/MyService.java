package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class MyService extends Service implements BluetoothConnection.connectionInterface {

    private BluetoothConnection mBluetoothConnection;

    ConnectedThread thread;

    Integer message = 0;


    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", "onStartCommand()");
        Log.d("MyService", "Thread name = " + Thread.currentThread().getName());

        int requestCode = intent.getIntExtra("REQUEST_CODE",0);
        Context context = getApplicationContext();
        String channelId = "default";
        String title = context.getString(R.string.app_name);

        message = intent.getIntExtra("LED_STATUS", 0);

        //デバイスのバージョン確認
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            //通知チャネル設定
            NotificationChannel channel = new NotificationChannel(
                    channelId, title, NotificationManager.IMPORTANCE_DEFAULT);
            //オブジェクトが初期化されている場合、新しい通知チャネルを作成する
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);

                Notification notification = new Notification.Builder(context, channelId)
                        .setContentTitle(title)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentText("MediaPlay")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setWhen(System.currentTimeMillis())
                        .build();
                startForeground(1, notification);

            }
        }

        String address = intent.getStringExtra("BT_address");

        //接続がまだ確立されていない場合は接続を開始、すでに接続が確立されている場合は一度閉じてから新しい接続を確立する
        if(mBluetoothConnection == null){
            connectDevice(address);
        } else {
            mBluetoothConnection.close();
            mBluetoothConnection = null;
            connectDevice(address);
        }

        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy(){
        thread.cancel();
    }
    //指定されたBluetoothデバイスとの接続を確立する
    private void connectDevice(String address){
        // BluetoothDeviceオブジェクトの取得
        Log.d("connectDevice ", "address = " + address);
        mBluetoothConnection = new BluetoothConnection(this, address);
        mBluetoothConnection.setConnectionListener(this);
        mBluetoothConnection.connect();
    }

    @Override
    //Bluetooth接続が確立されたときに、ConnectedThreadを生成してデータの送受信を開始する
    public void onBluetoothConnected() {
        thread = new ConnectedThread();
        thread.start();
    }

    @Override
    //Bluetooth接続が失敗したときにデバッグログメッセージを出力する
    public void onBluetoothConnectFailed() {
        Log.d(getClass().getSimpleName(), "onBluetoothConnectFailed");
    }
    //キーワードの存在チェック用変数の監視
    private class ConnectedThread extends Thread {
        boolean onActive = true;
        OutputStream out = Objects.requireNonNull(mBluetoothConnection.getOutputStream());

        public void run() {
            Log.d("ConnectedThread run", "Thread name = " + Thread.currentThread().getName());
            while (true) {
                try {
                    Thread.sleep(1000);
                    //arduinoにデータの送信
                    if (MainActivity.getSw()==1) {
                        out.write(message.toString().getBytes());
                        out.flush();
                        break;
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.d("ConnectedThread", "end");
        }

        // startの送信が終わった後0を送る
        public void cancel(){
            try {
                out.write(("0").getBytes());
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
            onActive = false;
            Log.d("ConnectedThread", "cancel");
        }
    }
}


