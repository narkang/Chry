package com.narkang.chry.core;

import android.text.TextUtils;
import android.util.Log;

import com.narkang.chry.bean.RequestBean;
import com.narkang.chry.bean.RequestParameter;
import com.narkang.chry.util.TypeUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class TypeCenter {

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> mRawMethods;
    private final ConcurrentHashMap<String, Class<?>> mClass;
    private final ConcurrentHashMap<String, Class<?>> mAnnotatedClasses;


    private static final TypeCenter ourInstance = new TypeCenter();

    public static TypeCenter getInstance() {
        return ourInstance;
    }

    private TypeCenter(){
        mAnnotatedClasses = new ConcurrentHashMap<>();
        mRawMethods = new ConcurrentHashMap<>();
        mClass = new ConcurrentHashMap<>();
    }

    public void register(Class<?> clazz) {
        registerClass(clazz);   //类型注册
        registerMethod(clazz);  //注册方法
    }

    private void registerMethod(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method: methods) {
            mRawMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
            ConcurrentHashMap<String, Method> map = mRawMethods.get(clazz);
            String methodId = TypeUtils.getMethodId(method);
            map.put(methodId, method);
        }
    }

    private void registerClass(Class<?> clazz) {
        String name = clazz.getName();
        mClass.putIfAbsent(name, clazz);
    }

    public Class<?> getClassType(String name)   {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        Class<?> clazz = mAnnotatedClasses.get(name);
        if (clazz == null) {
            try {
                clazz = Class.forName(name);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return clazz;
    }

    public Method getMethod(Class<?> clazz, RequestBean requestBean) {
        String name = requestBean.getMethodName();//
        if ( name!= null) {
            Log.i("david", "getMethod: 1======="+name);
            mRawMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
            ConcurrentHashMap<String, Method> methods = mRawMethods.get(clazz);
            Method method = methods.get(name);
            if (method != null) {
                Log.i("david", "getMethod: "+method.getName());
                return method;
            }
            int pos = name.indexOf('(');

            Class[] paramters = null;
            RequestParameter[] requestParameters = requestBean.getRequestParameter();
            if (requestParameters != null && requestParameters.length > 0) {
                paramters = new Class[requestParameters.length];
                for (int i=0;i<requestParameters.length;i++) {
                    paramters[i]=getClassType(requestParameters[i].getParameterClassName());
                }
            }
            method = TypeUtils.getMethod(clazz, name.substring(0, pos), paramters);
            methods.put(name, method);
            return method;
        }
        return null;


    }
}
