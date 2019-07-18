package com.narkang.chry.core;

import android.content.Context;

import com.google.gson.Gson;
import com.narkang.chry.Request;
import com.narkang.chry.Response;
import com.narkang.chry.annotion.ClassId;
import com.narkang.chry.bean.RequestBean;
import com.narkang.chry.bean.RequestParameter;
import com.narkang.chry.service.ChryService;
import com.narkang.chry.util.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Chry {

    //得到对象
    public static final int TYPE_NEW = 0;
    //得到单例
    public static final int TYPE_GET = 1;

    private Context mContext;
    private TypeCenter mTypeCenter;
    private ServiceConnectManager mSCM;

    Gson GSON = new Gson();

    private static final Chry ourInstance = new Chry();

    public static Chry getDefault() {
        return ourInstance;
    }

    private Chry() {
        mSCM = ServiceConnectManager.getInstance();
        mTypeCenter = TypeCenter.getInstance();
    }

    public void init(Context context){
        mContext = context.getApplicationContext();
    }

//    服务端调用
    public void register(Class<?> clazz) {
        mTypeCenter.register(clazz);
    }

//    客户端调用
    public void connect(Context context, Class<ChryService> clazz) {
        connectApp(context, null, clazz);
    }

    public void connectApp(Context context, String pkn, Class<ChryService> clazz) {
        init(context);
        mSCM.bind(context.getApplicationContext(), pkn, clazz);
    }

    public <T> T getInstance(Class<T> clazz, Object... parameters) {
        Response response = sendRequest(ChryService.class, clazz, null, parameters);
        return getProxy(ChryService.class, clazz);
    }

    private <T> T getProxy(Class<? extends ChryService> service, Class clazz) {
        ClassLoader classLoader = service.getClassLoader();
        T proxy = (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{clazz}, new ChryInvocationHandler(service, clazz));
        return proxy;
    }

    private <T> Response sendRequest(Class<ChryService> hermesServiceClass
            , Class<T> clazz, Method method, Object[] parameters) {
        RequestBean requestBean = new RequestBean();

        if (clazz.getAnnotation(ClassId.class) == null) {
            requestBean.setClassName(clazz.getName());
            requestBean.setResultClassName(clazz.getName());
        } else {
            requestBean.setClassName(clazz.getAnnotation(ClassId.class).value());
            requestBean.setResultClassName(clazz.getAnnotation(ClassId.class).value());
        }
        if (method != null) {
            requestBean.setMethodName(TypeUtils.getMethodId(method));
        }

        RequestParameter[] requestParameters = null;
        if (parameters != null && parameters.length > 0) {
            requestParameters = new RequestParameter[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                String parameterClassName = parameter.getClass().getName();
                String parameterValue = GSON.toJson(parameter);

                RequestParameter requestParameter = new RequestParameter(parameterClassName, parameterValue);
                requestParameters[i] = requestParameter;
            }
        }

        if (requestParameters != null) {
            requestBean.setRequestParameter(requestParameters);
        }

        Request request = new Request(GSON.toJson(requestBean), TYPE_GET);
        return mSCM.request(hermesServiceClass, request);

    }


    public <T> Response sendObjectRequest(Class<ChryService> hermesServiceClass
            , Class<T> clazz, Method method, Object[] parameters) {
        RequestBean requestBean = new RequestBean();

        //设置全类名
        if (clazz.getAnnotation(ClassId.class) == null) {
            requestBean.setClassName(clazz.getName());
            requestBean.setResultClassName(clazz.getName());
        } else {
//            返回类型的全类名
            requestBean.setClassName(clazz.getAnnotation(ClassId.class).value());
            requestBean.setResultClassName(clazz.getAnnotation(ClassId.class).value());
        }
        if (method != null) {
//            方法名 统一   传   方法名+参数名  getInstance(java.lang.String)
            requestBean.setMethodName(TypeUtils.getMethodId(method));
        }

        RequestParameter[] requestParameters = null;
        if (parameters != null && parameters.length > 0) {
            requestParameters = new RequestParameter[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                String parameterClassName = parameter.getClass().getName();
                String parameterValue = GSON.toJson(parameter);

                RequestParameter requestParameter = new RequestParameter(parameterClassName, parameterValue);
                requestParameters[i] = requestParameter;
            }
        }

        if (requestParameters != null) {
            requestBean.setRequestParameter(requestParameters);
        }

        Request request = new Request(GSON.toJson(requestBean), TYPE_NEW);
        return mSCM.request(hermesServiceClass, request);


    }
}
