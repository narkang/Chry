package com.narkang.server;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.narkang.chry.core.Chry;
import com.narkang.server.manager.UserManager;

/**
 *  IPC 服务端
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Chry.getDefault().register(UserManager.class);

    }

    public void getData(View view) {

        Toast.makeText(this, "服务端获取消息："+UserManager.getInstance().getData(), Toast.LENGTH_SHORT).show();

    }

    public void setData(View view) {

        UserManager.getInstance().setData("我是服务端");
        Toast.makeText(this, "服务端设置消息：" + UserManager.getInstance().getData(), Toast.LENGTH_SHORT).show();


    }
}
