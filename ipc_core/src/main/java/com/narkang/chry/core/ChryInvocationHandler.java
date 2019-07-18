package com.narkang.chry.core;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.narkang.chry.Response;
import com.narkang.chry.responce.ResponseBean;
import com.narkang.chry.service.ChryService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ChryInvocationHandler implements InvocationHandler {
    private static final String TAG = "alan";
    private Class clazz;
    private static final Gson GSON = new Gson();
    private Class hermeService;

    public ChryInvocationHandler(Class<? extends ChryService> service, Class clazz) {
        this.clazz = clazz;
        this.hermeService = service;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Response response = Chry.getDefault().sendObjectRequest(hermeService, clazz, method, args);
        if (!TextUtils.isEmpty(response.getData())) {
            ResponseBean responseBean = GSON.fromJson(response.getData(), ResponseBean.class);
            if (responseBean.getData() != null) {
                Object getUserResult = responseBean.getData();
                String data = GSON.toJson(getUserResult);
//
                Class stringgetUser = method.getReturnType();
                Object o = GSON.fromJson(data, stringgetUser);
                return o;

            }
        }
        return null;
    }
}
