package com.narkang.server.manager;

import com.narkang.chry.annotion.ClassId;

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
