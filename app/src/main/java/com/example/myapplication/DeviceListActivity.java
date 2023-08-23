package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.Set;

/**
 * ペアリングされているデバイスが一覧表示される
 *デバイスが選択されるとデバイスのMACアドレスが親デバイスに返される
 */
public class DeviceListActivity extends Activity {

    private static final String TAG = "DeviceListActivity";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBtAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        //ウィンドウの設定
        setContentView(R.layout.activity_device_list);

        //バックアウトした場合に備えて、結果をCANCELEDに設定する
        setResult(Activity.RESULT_CANCELED);

        //アレイアダプターを初期化する。すでにペアリングされているデバイス用と新しく検出されたデバイス用
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<>(this, R.layout.device_name);

        //ペアリングされたデバイスの ListView を検索して設定する
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        //ローカル Bluetooth アダプターを入手する
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        //AndroidバージョンがS以上で、Bluetooth接続の権限が許可されていない時Bluetooth接続の処理を行わない
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkSelfPermission != PackageManager.PERMISSION_GRANTED");
            return;
        }
        //現在ペアリングされているデバイスのセットを取得する
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        Log.d(TAG, "pairedDevices.size() = " + pairedDevices.size());

        //ペアリングされたデバイスがある場合は、それぞれをArrayAdapter に追加する
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d(TAG, "deviceName = " + device.getName() + ",Address = " + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            //デバイスの MAC アドレス (ビューの最後の 17 文字) を取得する
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            //結果のインテントを作成し、MAC アドレスを含める
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            //結果を設定してアクティビティを終了する
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
}

