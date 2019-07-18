package com.narkang.chry.responce;

import com.narkang.chry.bean.RequestBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ObjectResponseMake extends ResponseMake {
    private Method mMethod;

    private Object mObject;

    @Override
    protected Object invokeMethod() {

        try {
            return mMethod.invoke(mObject,mParameters);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void setMethod(RequestBean requestBean) {
        mObject = OBJECT_CENTER.getObject(resultClass.getName());
        Method method = typeCenter.getMethod(mObject.getClass(), requestBean);
        mMethod = method;
    }
}
