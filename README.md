
最近看了下饿了么开源框架[Hermes](https://github.com/Xiaofei-it/Hermes)，它是Android进程间IPC通信框架，可以避免AIDL的编写，使用接口代理的方式获取跨进程的数据，感觉挺有意思的，于是学习了下，自己总结了一套简易版的实现，当然还有许多要完善的，但是基本架构思想还是值得学习的。

----------------

## 项目结构
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190718145811322.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzE4MjQyMzkx,size_16,color_FFFFFF,t_70)

整个项目三个部分client，server，ipc_core，分别是客户端，服务端和IPC通信核心代码，其中client，server都依赖于ipc_core。

--------------------------------------

## 基本使用

服务端注册UserManager

```java

@ClassId("com.narkang.server.manager.UserManager")
public class UserManager {

    private static UserManager ourInstance = null;

    public static synchronized UserManager getInstance() {
        if(ourInstance == null){
            ourInstance = new UserManager();
        }
        return ourInstance;
    }

    public UserManager() {
    }

    private String data = "呵呵哒";

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}


Chry.getDefault().register(UserManager.class);
```

服务端Service注册

```java
 <service android:name="com.narkang.chry.service.ChryService" >
     <intent-filter>
         //服务端包名
         <action android:name="com.narkang.server" />
     </intent-filter>
 </service>
```

客户端使用

连接服务端

```java
Chry.getDefault().connectApp(this, "com.narkang.server", ChryService.class)
```

定义一个接口IUserManager

```java
@ClassId("com.narkang.server.manager.UserManager")
public interface IUserManager {

    String getData();

    void setData(String data);

}

IUserManager userManager = Chry.getDefault().getInstance(IUserManager.class);

```

在这里UserManager是一个单例，并且需要提供一个getInstance()方法，这个名字不能变，因为client去获取server的UserManager对象是通过这个方法名反射完成的，同时这个UserManager需要通过Chry注册，接着服务端需要注册一个server。而客户端想要使用需要首先连接服务端，接着定义一个接口IUserManager，接口里面的方法为需要操作UserManager里面的方法，需要一致，接着定义一个ClassId去保证IUserManager和UserManager的name一致。最后客户端就可以通过Chry.getDefault().getInstance(IUserManager.class)去获取服务端的UserManager代理对象。

--------------------

## 源码分析

 **1 .服务端注册**

```java
Chry.getDefault().register(UserManager.class)
```

这里Chry是一个单例对象，getDefault()就是获取其实例，来看register方法

```java
public void register(Class<?> clazz) {
    mTypeCenter.register(clazz);
}
```

可以看到注册转移到了TypeCenter中了

```java
public void register(Class<?> clazz) {
    registerClass(clazz);   //class类型注册
    registerMethod(clazz);  //注册方法
}
```
主要注册了class类和class类中的method

```java
private void registerClass(Class<?> clazz) {
	  String name = clazz.getName();
	  mClass.putIfAbsent(name, clazz);
}

private void registerMethod(Class<?> clazz) {
    Method[] methods = clazz.getMethods();
    for (Method method: methods) {
        mRawMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
        ConcurrentHashMap<String, Method> map = mRawMethods.get(clazz);
        String methodId = TypeUtils.getMethodId(method); //会根据method和parameter生成一个methodId
        map.put(methodId, method);
    }
}
```

注册的值是保存在了一个map中了

**2 .AIDL封装**

通常来说，不同的javabean之间想要进行跨进程通信，都要编写相应的AIDL文件。为了只编写一次AIDL文件就可以通用不同的javabean，这里就对javabean进行了进一步封装，在进行IPC调用之前，会将需要执行的方法、参数和返回值封装到一个Request中，然后转化为json，再将json和一个Type封装到Request中，然后进行传递，这样就只用编写一次AIDL，实现复用的目的。具体看代码实现

```java
// Request.aidl
package com.narkang.chry;

parcelable Request;

//Request.java
public class Request implements Parcelable {

    private String data; //1

    //    请求对象的类型
    private int type;
...
}

// Response.aidl
package com.narkang.chry;

parcelable Response;
public class Response implements Parcelable {
	//执行的结果也会转化为json存入data返回
    private String data; //2
}
...
//MyEventBusService.aidl
package com.narkang.chry;

import com.narkang.chry.Request;
import com.narkang.chry.Response;

interface MyEventBusService {
   Response send(in Request request); //3
}

```
注释1处的data是通过RequestBean转化为json存入的

```java
public class RequestBean {
    //请求单例的全类名
    private String className;
    //类型
    private String resultClassName;
    private String requestObject;
    //返回方法名字
    private String methodName;

//    参数
    private RequestParameter[] requestParameter;
...
}

```

注释2处的data是通过ResponseBean转化为json存入的

```java
public class ResponseBean {

    private Object data;

...
}
```

注释3处，客户端要和服务端通信调用send方法就行了。


**3 .客户端连接服务端**

```java
Chry.getDefault().connectApp(this, "com.narkang.server", ChryService.class);
```

如果服务端和客户端不在同一个进程是需要知道服务端的包名，否则包名不用传，可以这样使用

```java
Chry.getDefault().connect(this, ChryService.class);
```

connect内部也还是会调用到connectApp，看下这个方法的实现

```java
public void connectApp(Context context, String pkn, Class<ChryService> clazz) {
    init(context); //1
    mSCM.bind(context.getApplicationContext(), pkn, clazz); //2
}
```

看注释2，mSCM是一个ServiceConnectManager对象，主要是来绑定service和发送请求进行IPC调用。看下bind方法

```java
public void bind(Context context, String pkn, Class<ChryService> clazz) {

    ChryServiceConnection connection = new ChryServiceConnection(clazz);
    mChryServiceConnections.put(clazz, connection);

    Intent intent;
    //根据是否在同一个进程选择对应的Intent创建方式
    if (TextUtils.isEmpty(pkn)) {
        intent = new Intent(context, clazz);
    } else {
        intent = new Intent();
        intent.setClassName(pkn, clazz.getName());
    }
	//绑定服务
    context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
}
```

从这里可以看到是用来和服务端service进行绑定，绑定成功会添加到一个map中

```java
@Override
public void onServiceConnected(ComponentName name, IBinder service) {
    MyEventBusService myEventBusService = MyEventBusService.Stub.asInterface(service);
    mChryServices.put(mClass, myEventBusService);
}
```

**4 .客户端调用服务端的getInstance单例方法**

```java
IUserManager userManager = Chry.getDefault().getInstance(IUserManager.class);
```

看getInstance方法

```java
public <T> T getInstance(Class<T> clazz, Object... parameters) {
    Response response = sendRequest(ChryService.class, clazz, null, parameters); //1
    return getProxy(ChryService.class, clazz); //2
}
```

先看注释1处

```java
private <T> Response sendRequest(Class<ChryService> hermesServiceClass
        , Class<T> clazz, Method method, Object[] parameters) {
    RequestBean requestBean = new RequestBean();
	//根据是否有添加注解来存储对应的className和resultClassName
    if (clazz.getAnnotation(ClassId.class) == null) {
        requestBean.setClassName(clazz.getName());
        requestBean.setResultClassName(clazz.getName());
    } else {
        requestBean.setClassName(clazz.getAnnotation(ClassId.class).value());
        requestBean.setResultClassName(clazz.getAnnotation(ClassId.class).value());
    }
    //要执行的方法名
    if (method != null) {
        requestBean.setMethodName(TypeUtils.getMethodId(method));
    }

	//参数拼接
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
    return mSCM.request(hermesServiceClass, request);  //1

}

```

这个方法主要做的就是拼接class和method的签名，然后转化为json存放的Request对象中，接着调用注释1处的request，内部会调用AIDL里面的send方法进行IPC通信。此时会有一个TYPE_GET标志，看服务端的执行过程。

```java
private MyEventBusService.Stub mBinder=new MyEventBusService.Stub() {
    @Override
    public Response send(Request request){
//            对请求参数进行处理  生成Response结果返回
        ResponseMake responseMake = null;
        switch (request.getType()) {   //根据不同的类型，产生不同的策略
            case Chry.TYPE_GET://获取单例
                responseMake = new InstanceResponseMake();  //1
                break;
            case Chry.TYPE_NEW:
                responseMake = new ObjectResponseMake(); 
                break;
        }

        return responseMake.makeResponse(request); //2
    }
};
```

刚刚传入的是TYPE_GET标志，所以会执行注释1处的代码，这里其实用到了一个策略模式，InstanceResponseMake是ResponseMake的子类，而注释2处的makeResponse是在父类创建的，先看这个方法的实现

```java
public Response makeResponse(Request request) {
    RequestBean requestBean = GSON.fromJson(request.getData(), RequestBean.class);//1
    resultClass = typeCenter.getClassType(requestBean.getResultClassName());  //2
    RequestParameter[] requestParameters = requestBean.getRequestParameter();//3
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

    setMethod(requestBean); //4
    Object resultObject = invokeMethod(); //5
    ResponseBean responseBean = new ResponseBean(resultObject); //6
    String data = GSON.toJson(responseBean); //7
    Response response = new Response(data); //8
    return response;
}

```

在注释1处解析客户端传过来的RequestBean，注释2处获取resultClass，注释3处获取RequestParameter，后面会通过GSON.fromJson解析处参数，注释4处在子类InstanceResponseMake中实现，注释5处会执行客户端需要执行的方法，注释6，7，8会将返回结果封装到ResponseBean，然后通过GSON.toJson转成string，然后封装到response返回给客户端。重点看注释4和5，先看注释4处在InstanceResponseMake中的实现

```java
@Override
 protected void setMethod(RequestBean requestBean) {
     RequestParameter[] requestParameters = requestBean.getRequestParameter();

     Class<?>[] parameterTypes = null;
     //下面这段获取参数class类型
     if (requestParameters != null && requestParameters.length > 0) {
         parameterTypes = new Class<?>[requestParameters.length];
         for (int i = 0; i < requestParameters.length; ++i) {
             parameterTypes[i] = typeCenter.getClassType(requestParameters[i].getParameterClassName());
         }
     }
     String methodName = requestBean.getMethodName(); //可能出现重载
     Method method = TypeUtils.getMethodForGettingInstance(resultClass, methodName, parameterTypes); //1
     mMethod = method;
 }
```

主要看注释1处，传入解析到的resultClass，方法名methodName，参数parameterTypes到TypeUtils.getMethodForGettingInstance去获取Method对象，来看下其实现

```java
public static Method getMethodForGettingInstance(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
    Method[] methods = clazz.getMethods();
    Method result = null;
    if (parameterTypes == null) {
        parameterTypes = new Class[0];
    }
    for (Method method : methods) {
        String tmpName = method.getName();
        if (tmpName.equals("getInstance")) {
            if (classAssignable(method.getParameterTypes(), parameterTypes)) {
                result = method;
                break;
            }
        }
    }

    if (result != null) {
        if (result.getReturnType() != clazz) {
			return null;
        }
        return result;
    }
    return null;
}
```

这里会获取clazz的所有method，然后去遍历所有method，然后去找是否有getInstance方法，有的话接着就去比较里面的parameterTypes，比较上了，就取这个method，接着就去比较result.getReturnType()是否和clazz相等，然后返回result，接着会保存在一个全局mMethod。接着看上面注释5的代码

```java
protected Object invokeMethod() {

    Object object = null;
    try {
        object = mMethod.invoke(null, mParameters);  //1
        //            保存对象
        OBJECT_CENTER.putObject(object.getClass().getName(), object);  //2
    }catch (IllegalAccessException e) {
        e.printStackTrace();
    } catch (InvocationTargetException e) {
        e.printStackTrace();
    }


    return null;
}
```

注释1通过反射执行这个单例对象，然后在注释2处，将这个对象保存在全局OBJECT_CENTER中了。


**5. 客户端获取代理对象**

```java
private <T> T getProxy(Class<? extends ChryService> service, Class clazz) {
    ClassLoader classLoader = service.getClassLoader();
    T proxy = (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{clazz}, new ChryInvocationHandler(service, clazz));
    return proxy;
}
```

也就是说客户端获取的都是代理对象，来看下其处理过程ChryInvocationHandler

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) {
    Response response = Chry.getDefault().sendObjectRequest(hermeService, clazz, method, args); //1
...
}
```

这里注释1处会调用服务端的方法，再来看服务器处理方法

```java
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
                responseMake = new ObjectResponseMake();  //2
                break;
        }

        return responseMake.makeResponse(request);
    }
};
```

直接看ObjectResponseMake对象里面的处理过程

```java
 @Override
 protected void setMethod(RequestBean requestBean) {
     mObject = OBJECT_CENTER.getObject(resultClass.getName());
     Method method = typeCenter.getMethod(mObject.getClass(), requestBean);
     mMethod = method;
 }
```

这里先会去获取对应的Method对象，然后保存在全局mMethod中，接着就会去执行这个method

```java
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
```

执行完之后就和之前一样转化为json，保存在Response中，接着再来看这个代理对象

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) {
    Response response = Chry.getDefault().sendObjectRequest(hermeService, clazz, method, args); //1
    if (!TextUtils.isEmpty(response.getData())) {
        ResponseBean responseBean = GSON.fromJson(response.getData(), ResponseBean.class);//2
        if (responseBean.getData() != null) {
            Object getUserResult = responseBean.getData();
            String data = GSON.toJson(getUserResult);
//
            Class stringgetUser = method.getReturnType();
            Object o = GSON.fromJson(data, stringgetUser); //3
            return o;

        }
    }
    return null;
}
```
执行成功会在注释2处解析responseBean，接着会在注释3处解析method的返回类型，返回给调用者使用。

## 总结

源码分析到了这里总结下先会去执行服务器注册对象的getInstance方法，然后将返回对象保存在全局map中，接着客户端在调用接口方法时候会去代理对象中执行服务器的method，客户端就可以获取到执行结果。这样一来对于用户来说就是无感知的，就像调用同一进程方法，获取到服务端方法执行结果。

