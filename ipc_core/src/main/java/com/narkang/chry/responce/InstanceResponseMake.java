package com.narkang.chry.responce;

import com.narkang.chry.bean.RequestBean;
import com.narkang.chry.bean.RequestParameter;
import com.narkang.chry.util.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InstanceResponseMake extends ResponseMake {
    private Method mMethod;
    @Override
    protected Object invokeMethod() {

        Object object = null;
        try {
            object = mMethod.invoke(null, mParameters);
            //            保存对象
            OBJECT_CENTER.putObject(object.getClass().getName(), object);
        }catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    protected void setMethod(RequestBean requestBean) {
        RequestParameter[] requestParameters = requestBean.getRequestParameter();

        Class<?>[] parameterTypes = null;
        if (requestParameters != null && requestParameters.length > 0) {
            parameterTypes = new Class<?>[requestParameters.length];
            for (int i = 0; i < requestParameters.length; ++i) {
                parameterTypes[i] = typeCenter.getClassType(requestParameters[i].getParameterClassName());
            }
        }
        String methodName = requestBean.getMethodName(); //可能出现重载
        Method method = TypeUtils.getMethodForGettingInstance(resultClass, methodName, parameterTypes);
        mMethod = method;
    }
}
