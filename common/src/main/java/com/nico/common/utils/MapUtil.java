package com.nico.common.utils;

import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MapUtil {
    public static String uppercaseFirstChar(String str){
        if(StringUtils.isEmpty(str)){
            return str;
        }else {
            String f=str.substring(0,1).toUpperCase();
            String s=str.substring(1);
            return  f+s;
        }
    }

    public static String makeSetMethodName(String propertyname){
        return "set"+uppercaseFirstChar(propertyname);
    }


    /**
     *
     * @方法名 ：resultset2List<br>
     * @方法描述 ：根据结果集（多条数据）映射 到 实体类集合<br>
     * @创建者 ：nico
     * @创建时间 ：2020年3月10日21:54:51 <br>
     * @param <T>
     *            ：泛型
     * @param tClass
     *            ：实体类的Class
     * @param resultSet
     *            ：查询的结果集
     * @return 返回类型 ：List<T>
     */

    public static  <T> List<T> resultset2List(Class<T>tClass, ResultSet resultSet) throws SQLException,IllegalArgumentException,IllegalAccessException, InvocationTargetException,SecurityException,NoSuchMethodException,InstantiationException {
        ResultSetMetaData resultSetMetaData=null;
        String temp="";
        T t=null;
        List<T> list=new ArrayList<>();
        Method s=null;
        resultSetMetaData=resultSet.getMetaData();
        while (resultSet!=null&&resultSet.next()){
            t=tClass.newInstance();
            for(int i=1;i<=resultSetMetaData.getColumnCount();i++){
                temp=resultSetMetaData.getColumnLabel(i);
                String temp1=temp.replaceAll("_","").toLowerCase();
                for(Field filed:tClass.getDeclaredFields()){
                    String filedname=filed.getName().toLowerCase();
                    if(filedname.equals(temp1)){
                        s=tClass.getDeclaredMethod(makeSetMethodName(filed.getName()),filed.getType());
                        s.invoke(t,resultSet.getObject(temp));
                        break;///匹配到跳出当前循环
                    }
                }
            }
            list.add(t);
        }
        return list;
    }

    public static <T> T result2Object(Class<T> tClass,ResultSet resultSet) throws SQLException,IllegalArgumentException,IllegalAccessException,InvocationTargetException,SecurityException,NoSuchMethodException,InstantiationException{
        return  resultset2List(tClass,resultSet).get(0);
    }
}
