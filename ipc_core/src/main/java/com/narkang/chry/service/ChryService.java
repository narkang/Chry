package com.narkang.chry.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.narkang.chry.MyEventBusService;
import com.narkang.chry.Request;
import com.narkang.chry.Response;
import com.narkang.chry.core.Chry;
import com.narkang.chry.responce.InstanceResponseMake;
import com.narkang.chry.responce.ObjectResponseMake;
import com.narkang.chry.responce.ResponseMake;

public class ChryService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private MyEventBusService.Stub mBinder=new MyEventBusService.Stub() {
        @Override
        public Response send(Request request){
//            对请求参数进行处理  生成Response结果返回
            ResponseMake responseMake = null;
            switch (request.getType()) {   //根据不同的类型，产生不同的策略
                case Chry.TYPE_GET://获取单例
                    responseMake = new InstanceResponseMake();
                    break;
                case Chry.TYPE_NEW:
                    responseMake = new ObjectResponseMake();
                    break;
            }

            return responseMake.makeResponse(request);
        }
    };
}
