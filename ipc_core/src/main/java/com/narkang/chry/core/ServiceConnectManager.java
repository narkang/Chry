package com.narkang.chry.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.narkang.chry.MyEventBusService;
import com.narkang.chry.Request;
import com.narkang.chry.Response;
import com.narkang.chry.service.ChryService;

import java.util.concurrent.ConcurrentHashMap;

public class ServiceConnectManager {

    private static final ServiceConnectManager sInstance = new ServiceConnectManager();
    private final ConcurrentHashMap<Class<? extends ChryService>, MyEventBusService>
            mChryServices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends ChryService>, ChryServiceConnection>
            mChryServiceConnections = new ConcurrentHashMap<>();

    public static ServiceConnectManager getInstance() {
        return sInstance;
    }

    private ServiceConnectManager() {
    }

    public void bind(Context context, String pkn, Class<ChryService> clazz) {

        ChryServiceConnection connection = new ChryServiceConnection(clazz);
        mChryServiceConnections.put(clazz, connection);

        Intent intent;
        if (TextUtils.isEmpty(pkn)) {
            intent = new Intent(context, clazz);
        } else {
            intent = new Intent();
            intent.setClassName(pkn, clazz.getName());
        }

        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public Response request(Class<ChryService> hermesServiceClass, Request request) {
        //从缓存中获取binder代理对象，发送请求
        MyEventBusService eventBusService = mChryServices.get(hermesServiceClass);
        if (eventBusService != null) {
            try {
                Response response = eventBusService.send(request);
                return response;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private class ChryServiceConnection implements ServiceConnection {

        private Class<? extends ChryService> mClass;

        ChryServiceConnection(Class<? extends ChryService> clazz) {
            this.mClass = clazz;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyEventBusService myEventBusService = MyEventBusService.Stub.asInterface(service);
            mChryServices.put(mClass, myEventBusService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mChryServices.remove(mClass);
        }
    }

}
