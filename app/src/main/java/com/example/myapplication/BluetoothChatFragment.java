package com.example.myapplication;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class BluetoothChatFragment  extends Fragment {
    private BluetoothAdapter mBluetoothAdapter = null;

    private Button mButtonStart;
    private Button mButtonStop;

    String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //アダプターがnullの場合、Bluetoothはサポートされていない
        FragmentActivity activity = getActivity();
        if (mBluetoothAdapter == null && activity != null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //Bluetoothアダプターが存在しない状態での処理を防ぐ
        if (mBluetoothAdapter == null) {
            return;
        }
        //Bluetoothがオンになっていない場合は、Bluetoothを有効にするように要求する
        //setupChat()がonActivityResultの間に呼び出される
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mStartBluetoothAdapterEnable.launch(enableIntent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }
    //Bluetoothを有効にしなかったか、エラーが発生した場合
    ActivityResultLauncher<Intent> mStartBluetoothAdapterEnable = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) {
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                                Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                }
            });

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mButtonStart = view.findViewById(R.id.button_start);
        mButtonStart.setOnClickListener( v -> {
            // アドレスが与えられなかったとき
            if(address == null || address.length() == 0){
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.no_btAddress,
                            Toast.LENGTH_SHORT).show();
                    Log.d("button_start", getString(R.string.no_btAddress));
                }
            } else {
                Intent intent = new Intent(getActivity(), MyService.class);
                intent.putExtra("REQUEST_CODE", 1);
                intent.putExtra("BT_address", address);
                intent.putExtra("LED_STATUS", 1);


                // Serviceの開始
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(intent);
                }
            }
        });
        mButtonStop=view.findViewById(R.id.button_stop);

        mButtonStop.setOnClickListener( v ->{
            Log.d("debug","button_stop");
            Intent intent = new Intent(getActivity(),MyService.class);
            //ストップサービスの処理の呼び出し
            requireActivity().stopService(intent);
        });
    }

    @Override
    //指定されたメニューリソースを使用してメニュー項目を生成する
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect: {
                Log.d("onOptionsItemSelected", "push R.id.connect");
                // DeviceListActivityを起動してデバイスを表示し、スキャンを実行する
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                mGetBluetoothAddress.launch(serverIntent);
                return true;
            }
        }
        return false;
    }
    //DeviceListActivityを動かす
    ActivityResultLauncher<Intent> mGetBluetoothAddress = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Bundle extras;
                    if (result.getData() != null) {
                        extras = result.getData().getExtras();
                        address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        Log.d("mGetBluetoothAddress.getData ", "address = " + address);
                    }
                }
            });

}
