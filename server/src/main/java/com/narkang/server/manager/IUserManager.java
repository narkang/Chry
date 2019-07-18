package com.narkang.server.manager;

import com.narkang.chry.annotion.ClassId;

//接口的方式  描述 一个类
@ClassId("com.narkang.server.manager.UserManager")
public interface IUserManager {

    String getData();

    void setData(String data);

}
