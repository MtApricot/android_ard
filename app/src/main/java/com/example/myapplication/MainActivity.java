package com.example.myapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 0;

    private TextView textView;
    private int lang ;
    private Button mButton;
    private  static Integer kai=0;

    //キーワードの存在チェック用変数
    private  static Integer sw=0;

    public static int getSw() {
        return sw;
    }


    public void setSw ( int value){
        sw = value;
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::onActivityResult);


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //状態が保存されていない場合、トランザクションを行う
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothChatFragment fragment = new BluetoothChatFragment();
            transaction.replace(R.id.main_layout, fragment);
            transaction.commit();
        }
        //ユーザーが実行時、権限を取り消したかどうかを確認する
        if (!checkPermissions()) {
            requestPermissions();
        }
        // 認識結果を表示させる
        textView =  findViewById(R.id.textView);

        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(v -> {
            // 音声認識を開始
            speech();
        });

    }

    private boolean checkPermissions() {
        int permissionState = 0;
        //Bluetooth接続のパーミッションの状態を確認
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionState = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT);
        }
        Log.i(TAG, "android.os.Build.VERSION.SDK_INT = " + android.os.Build.VERSION.SDK_INT + ",checkPermissions = " + permissionState);    //  PERMISSION_GRANTED = 0
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermissions() {
        boolean shouldProvideRationale = false;
        //パーミッション要求
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_CONNECT);
        }


        if (shouldProvideRationale) {   // 今後は確認しない をチェックしているしているか判断する
        } else {
            Log.i(TAG, "Requesting permission");
            //許可を要求、デバイス・ポリシーが設定されていれば、自動応答される可能性がある
            //権限を特定の状態に設定する
            //今後質問しない
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
        }
    }
    //音声認識
    private void speech(){
        // 音声認識のIntentインスタンス
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1000000000);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "音声を入力");

        try {
            // インテント発行
            resultLauncher.launch(intent);
        }
        catch (ActivityNotFoundException e) {
            e.printStackTrace();
            textView.setText(R.string.error);
        }
    }
    //認識で取得したデータの解析
    private  void onActivityResult(ActivityResult result){
        //アクティビティの実行結果が正常だった場合、結果のデータを取得する
        if(result.getResultCode()== Activity.RESULT_OK){
            Intent resultData = result.getData();
            //認識された文字列のリストを取得する
            if (resultData!=null){
                ArrayList candidates =
                        resultData.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                //リストや配列内の要素が存在するか確認
                if (candidates.size()>0) {
                    String m = (String) candidates.get(0);
                    int len = candidates.size();

                    Integer ii = Integer.valueOf(len);
                    String l = ii.toString();
                    //キーワードが含まれているか確認する
                    if (m.contains("金")||m.contains("銀行")||m.contains("カード")||m.contains("誰")||m.contains("返済")) {
                        sw = 1;
                        //キーワード出現回数
                        kai=kai+1;
                        String keikoku= "詐欺の可能性があります"+String.valueOf(kai)+"回目";
                                textView.setText(keikoku);
                    }
                    speech();
                }
            }
        }
    }
}
