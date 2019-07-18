package com.narkang.chry.responce;

import com.google.gson.Gson;
import com.narkang.chry.Request;
import com.narkang.chry.Response;
import com.narkang.chry.bean.RequestBean;
import com.narkang.chry.bean.RequestParameter;
import com.narkang.chry.core.ObjectCenter;
import com.narkang.chry.core.TypeCenter;


public abstract class ResponseMake {
    //UserManage  的Class
    protected Class<?> resultClass;
    // getInstance()  参数数组
    protected Object[] mParameters;

    Gson GSON = new Gson();

    protected TypeCenter typeCenter = TypeCenter.getInstance();

    protected static final ObjectCenter OBJECT_CENTER = ObjectCenter.getInstance();



    protected abstract Object invokeMethod()  ;

    protected abstract void setMethod(RequestBean requestBean);

    public Response makeResponse(Request request) {
        RequestBean requestBean = GSON.fromJson(request.getData(), RequestBean.class);
        resultClass = typeCenter.getClassType(requestBean.getResultClassName());
        RequestParameter[] requestParameters = requestBean.getRequestParameter();
        if (requestParameters != null && requestParameters.length > 0) {
            mParameters = new Object[requestParameters.length];
            for (int i=0;i<requestParameters.length;i++) {
                RequestParameter requestParameter = requestParameters[i];
                Class<?> clazz = typeCenter.getClassType(requestParameter.getParameterClassName());
                mParameters[i] =  GSON.fromJson(requestParameter.getParameterValue(), clazz);
            }
        }else {
            mParameters = new Object[0];
        }

        setMethod(requestBean);
        Object resultObject = invokeMethod();
        ResponseBean responseBean = new ResponseBean(resultObject);
        String data = GSON.toJson(responseBean);
        Response response = new Response(data);
        return response;
    }
}
