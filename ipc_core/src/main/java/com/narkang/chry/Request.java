package com.narkang.chry;

import android.os.Parcel;
import android.os.Parcelable;

public class Request implements Parcelable {

    private String data;

    //    请求对象的类型
    private int type;

    public Request(String data, int type) {
        this.data = data;
        this.type = type;
    }

    //反序列化
    protected Request(Parcel in) {
        data = in.readString();
        type = in.readInt();
    }

    public static final Creator<Request> CREATOR = new Creator<Request>() {
        @Override
        public Request createFromParcel(Parcel in) {
            return new Request(in);
        }

        @Override
        public Request[] newArray(int size) {
            return new Request[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //序列化
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
        dest.writeInt(type);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
