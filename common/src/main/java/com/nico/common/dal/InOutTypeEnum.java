package com.nico.common.dal;

public enum InOutTypeEnum {

    intype("IN"),

    outtype("OUT");

    private String type;

    private InOutTypeEnum(String Type){
        this.type=Type;
    }

    public String getType(){
        return this.type;
    }
}
