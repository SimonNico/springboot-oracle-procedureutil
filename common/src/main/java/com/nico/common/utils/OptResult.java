package com.nico.common.utils;

import java.io.Serializable;

public class OptResult implements Serializable {

    private static final long serialVersionUID = -5473633069447049985L;

    private String result;

    private String msg;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
