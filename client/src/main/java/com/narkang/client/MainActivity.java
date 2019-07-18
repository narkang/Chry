package com.narkang.client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.narkang.chry.core.Chry;
import com.narkang.chry.service.ChryService;
import com.narkang.client.manager.IUserManager;

/**
 * IPC客户端
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Chry.getDefault().connectApp(this, "com.narkang.server", ChryService.class);
    }

    public void getData(View view) {

        IUserManager userManager = Chry.getDefault().getInstance(IUserManager.class);

        Toast.makeText(this, "客户端获取消息:" + userManager.getData(), Toast.LENGTH_SHORT).show();

    }

    public void setData(View view) {

        IUserManager userManager = Chry.getDefault().getInstance(IUserManager.class);
        userManager.setData("我是客户端");
        Toast.makeText(this, "客户端设置消息："+userManager.getData(), Toast.LENGTH_SHORT).show();


    }
}
