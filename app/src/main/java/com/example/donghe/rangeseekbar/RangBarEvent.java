package com.example.donghe.rangeseekbar;

/**
 * @author hedong
 *
 * Created by hedong on 2016/10/09
 */
public class RangBarEvent {

    private int msg;
    private int type;

    public RangBarEvent(int str, int type) {
        msg = str;
        this.type = type;
    }

    public int getMsg() {
        return msg;
    }

    public int getType(){
        return type;
    }

}
