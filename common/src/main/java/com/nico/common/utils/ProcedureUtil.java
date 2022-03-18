package com.nico.common.utils;

import com.nico.common.dal.InOutTypeEnum;
import com.nico.common.dal.ProcedureParam;
import com.nico.common.dal.ProcedureParamType;
import oracle.jdbc.internal.OracleTypes;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * 存储过程工具
 * created by nico
 * date:2020年3月10日
 * 注意：所有的参数都要继承IProcedureParam,参数一定要添加注解ProcedureParamType
 */
public class ProcedureUtil {

    /***
     * 执行存储过程有返回值的 且结果是OptResult结果
     * @param procSql
     * @param param
     * @param jdbcTemplate
     * @return result & msg
     * @throws SQLException
     * @throws DataAccessException
     */
    public static OptResult execProcReturnOptResult(String procSql, final ProcedureParam param, JdbcTemplate jdbcTemplate) throws SQLException, DataAccessException {
        OptResult optResult = jdbcTemplate.execute(procSql, new CallableStatementCallback<OptResult>() {
            @Override
            public OptResult doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                OptResult optResult1 = new OptResult();
                Map<String, Integer> map = new HashMap<>();
                try {
                    Field[] fields = param.getClass().getDeclaredFields();////反射参数获取参数属性数组
                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            field.setAccessible(true);
                            if (procedureParamType == null) continue;///无注解的忽视
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注释里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        cs.setString(procedureParamType.paramindex(), (String)field.get(param));
                                        break;
                                    case OracleTypes.DATE:
                                        cs.setDate(procedureParamType.paramindex(),utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                /////为Out的时候
                                if (procedureParamType.Oracletype() == OracleTypes.VARCHAR) {
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.VARCHAR);
                                    if (field.getName().toLowerCase().contains("result")) {
                                        map.put("result", procedureParamType.paramindex());////记录参数的序号 以便返回
                                    } else if (field.getName().toLowerCase().contains("msg")) {
                                        map.put("msg", procedureParamType.paramindex());
                                    } else if (field.getName().toLowerCase().contains("message")) {
                                        map.put("message", procedureParamType.paramindex());
                                    }
                                }
                            }

                        }
                        cs.execute();
                        if (map.containsKey("result")) optResult1.setResult(cs.getString(map.get("result")));
                        else {
                            throw new SQLException("参数中未设置名为result的返回值！");
                        }
                        if (map.containsKey("msg")) optResult1.setMsg(cs.getString(map.get("msg")));
                        else if (map.containsKey("message")) optResult1.setMsg(cs.getString(map.get("message")));
                        else {
                            throw new SQLException("参数中未设置名称为msg或者message的返回值！");
                        }
                    }
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLException(ex.toString());
                }
                return optResult1;
            }
        });
        return optResult;
    }

    /**
     * 无返回值的存储过程
     *
     * @param procSql
     * @param param
     * @param jdbcTemplate
     * @return 处理结果；1 正常 0 异常
     */
    public static Integer execProcedureWithoutReturn(String procSql, final ProcedureParam param, JdbcTemplate jdbcTemplate) {
        Integer i = jdbcTemplate.execute(procSql, new PreparedStatementCallback<Integer>() {
            @Override
            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                Integer result = 0;
                try {
                    Field[] fields = param.getClass().getDeclaredFields();
                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            if (procedureParamType == null) continue;///无注解的忽视
                            field.setAccessible(true);
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注释里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        ps.setString(procedureParamType.paramindex(), (String)field.get(param));
                                        break;
                                    case OracleTypes.DATE:
                                        ps.setDate(procedureParamType.paramindex(), utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        ps.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        result = ps.execute() ? 1 : 0;
                    }

                } catch (SQLException sqlex) {
                    throw sqlex;
                } catch (IllegalAccessException ix) {
                    throw new SQLException(ix.toString());
                }
                return result;
            }
        });
        return i;
    }

    public static Object execProcedureReturnTObject(String procsql, final ProcedureParam param, JdbcTemplate jdbcTemplate) {
        final Map<String, Integer> map = new HashMap<>();
        Object t = jdbcTemplate.execute(procsql, new CallableStatementCallback<Object>() {
            @Override
            public Object doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                Class tClass = null;
                try {
                    Field[] fields = param.getClass().getDeclaredFields();////反射参数获取参数属性数组
                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            if (procedureParamType == null) continue;///无注解的忽视
                            field.setAccessible(true);
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注释里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        cs.setString(procedureParamType.paramindex(), (String)field.get(param));/////null 不能toString()
                                        break;
                                    case OracleTypes.DATE:
                                        cs.setDate(procedureParamType.paramindex(),utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                /////为Out的时候
                                if (procedureParamType.Oracletype() == OracleTypes.CURSOR) {
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.CURSOR);
                                    tClass = procedureParamType.T_CLASS();
                                    map.put("records", procedureParamType.paramindex());////记录参数的序号 以便返回
                                }else if(procedureParamType.Oracletype() == OracleTypes.VARCHAR){
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.VARCHAR);
                                    tClass = procedureParamType.T_CLASS();
                                    map.put("records", procedureParamType.paramindex());////记录参数的序号 以便返回
                                }

                            }
                        }
                        cs.execute();
                        if(tClass==String.class){
                            return (String) cs.getString(map.get("records"));
                        }else {
                            ResultSet resultSet = (ResultSet) cs.getObject(map.get("records"));
                            return result2Object(tClass, resultSet);
                        }
                    }
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.toString());
                } catch (InvocationTargetException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (SecurityException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (NoSuchMethodException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (InstantiationException ex) {
                    throw new SQLDataException(ex.toString());
                }
                return null;
            }
        });
        return t;
    }

    public static List<Map<String,Object>> execProcedureReturenTMap(String procsql, final  ProcedureParam param, JdbcTemplate jdbcTemplate) {
        final Map<String, Integer> map = new HashMap<>();
        List<Map<String,Object>> list1 = jdbcTemplate.execute(procsql, new CallableStatementCallback<List<Map<String,Object>>>() {
            @Override
            public List<Map<String,Object>> doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                Class tClass = null;
                try {
                    Field[] fields = param.getClass().getDeclaredFields();////反射参数获取参数属性数组
                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            if (procedureParamType == null) continue;///无注解的忽视
                            field.setAccessible(true);
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注释里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        cs.setString(procedureParamType.paramindex(), (String)field.get(param));
                                        break;
                                    case OracleTypes.DATE:
                                        cs.setDate(procedureParamType.paramindex(), utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                /////为Out的时候
                                if (procedureParamType.Oracletype() == OracleTypes.CURSOR) {
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.CURSOR);
                                    tClass = procedureParamType.T_CLASS();
                                    map.put("records", procedureParamType.paramindex());////记录参数的序号 以便返回
                                }
                            }
                        }
                        cs.execute();
                        ResultSet resultSet = (ResultSet) cs.getObject(map.get("records"));
                        Map<String, List<Map<String, Object>>> map1=new HashMap<>();
                        ResultSetMetaData md = resultSet.getMetaData();
                        int columnCount = md.getColumnCount();
                        List<Map<String,Object>> list=new ArrayList<>();
                        while (resultSet.next()) {
                            Map<String,Object> rowData = new HashMap<String,Object>();
                            for (int i = 1; i <= columnCount; i++) {
                                rowData.put(md.getColumnName(i), resultSet.getObject(i));
                            }
                            list.add(rowData);
                        }
                        return list;
                    }
                    else{
                        return null;
                    }
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.toString());
                } catch (SecurityException ex) {
                    throw new SQLDataException(ex.toString());
                }
            }
        });
        return list1;
    }




    public static List<?> execProcedureReturenTList(String procsql, final ProcedureParam param, JdbcTemplate jdbcTemplate) {
        final Map<String, Integer> map = new HashMap<>();
        List<?> list1 = jdbcTemplate.execute(procsql, new CallableStatementCallback<List<?>>() {
            @Override
            public List<?> doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                Class tClass = null;
                try {
                    Field[] fields = param.getClass().getDeclaredFields();////反射参数获取参数属性数组
                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            if (procedureParamType == null) continue;///无注解的忽视
                            field.setAccessible(true);
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注释里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        cs.setString(procedureParamType.paramindex(), (String)field.get(param));
                                        break;
                                    case OracleTypes.DATE:
                                        cs.setDate(procedureParamType.paramindex(), utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                /////为Out的时候
                                if (procedureParamType.Oracletype() == OracleTypes.CURSOR) {
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.CURSOR);
                                    tClass = procedureParamType.T_CLASS();
                                    map.put("records", procedureParamType.paramindex());////记录参数的序号 以便返回
                                }
                            }
                        }
                        cs.execute();
                        ResultSet resultSet = (ResultSet) cs.getObject(map.get("records"));
                        return resultset2List(tClass, resultSet);
                    }
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.toString());
                } catch (InvocationTargetException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (SecurityException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (NoSuchMethodException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (InstantiationException ex) {
                    throw new SQLDataException(ex.toString());
                }
                return null;
            }
        });
        return list1;
    }

    /**
     * 存储过程分页查询
     *
     * @param prosql:执行的存储过程语句类似{call xxxxx(?,?,?,?)}
     * @param param                   传入的参数 参数里一定要传入 startrow ,endrow和totalqty 且类型为int 注解里是Number
     * @param jdbcTemplate
     * @param
     * @param
     * @return 分页结果集
     */
    public static <T> ListResult<T> execProcedureReturnTListResult(Class<T> tClass, String prosql, final ProcedureParam param, JdbcTemplate jdbcTemplate) {
        final Map<String, Integer> map = new HashMap<>();

        ListResult<T> listResult = jdbcTemplate.execute(prosql, new CallableStatementCallback<ListResult<T>>() {
            @Override
            public ListResult<T> doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                int pageIndex = 0;
                int pageSize = 0;
                int startrow = 0;
                int endrow = 0;
                Class tClass = null;
                try {
                    Field[] fildes = param.getClass().getDeclaredFields();
                    for (Field field : fildes) {
                        ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                        if (procedureParamType == null) continue;///无注解的忽视
                        field.setAccessible(true);
                        if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注释里的参数类型为IN
                            switch (procedureParamType.Oracletype()) {
                                case OracleTypes.VARCHAR:
                                    cs.setString(procedureParamType.paramindex(), (String)field.get(param));
                                    break;
                                case OracleTypes.DATE:
                                    cs.setDate(procedureParamType.paramindex(), utilDate2sqlDate((java.util.Date) field.get(param)));
                                    break;
                                case OracleTypes.NUMBER:
                                    cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                    if (field.getName().toLowerCase().equals("startrow"))
                                        startrow = (int) field.get(param);
                                    if (field.getName().toLowerCase().equals("endrow")) endrow = (int) field.get(param);
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            switch (procedureParamType.Oracletype()) {
                                case OracleTypes.NUMBER:
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.NUMBER);
                                    if (field.getName().toLowerCase().equals("totalqty")) {
                                        map.put("totalqty", procedureParamType.paramindex());
                                    }
                                    break;
                                case OracleTypes.CURSOR:
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.CURSOR);
                                    tClass = procedureParamType.T_CLASS();
                                    map.put("records", procedureParamType.paramindex());////记录参数的序号 以便返回

                            }
                        }

                    }
                    cs.execute();
                    int total = cs.getInt(map.get("totalqty"));
                    ResultSet resultSet = (ResultSet) cs.getObject(map.get("records"));
                    List<T> list = resultset2List(tClass, resultSet);

                    pageSize = (endrow - startrow) == 0 ? 1 : (endrow - startrow);
                    pageIndex = (startrow + pageSize) / pageSize;
                    int pages;
                    ListResult<T> result = new ListResult<T>();
                    if (total % pageSize == 0) {
                        pages = total / pageSize;
                    } else {

                        pages = total / pageSize + 1;
                    }
                    result.setPages(pages);
                    result.setPageIndex(pageIndex);
                    result.setRecords(total);
                    result.setRows(list);
                    return result;
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.toString());
                } catch (InvocationTargetException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (SecurityException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (NoSuchMethodException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (InstantiationException ex) {
                    throw new SQLDataException(ex.toString());
                }
            }
        });
        return listResult;

    }

    /**
     * 返回多个集合
     * @param procsql
     * @param param 参数一定要有out类型，只返回Cursor，T_Class 一定要定义
     * @param jdbcTemplate
     * @return
     */
    public static Map<String, List<?>> execProcdureReturnMultiResultSet(String procsql, final ProcedureParam param, JdbcTemplate jdbcTemplate) {
        Map<String, List<?>> map = jdbcTemplate.execute(procsql, new CallableStatementCallback<Map<String, List<?>>>() {
            @Override
            public Map<String, List<?>> doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                Map<String, Map.Entry<Class, Integer>> mapMap = new HashMap<>();
                try {
                    Field[] fields = param.getClass().getDeclaredFields();

                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            if (procedureParamType == null) continue;///无注解的忽视
                            field.setAccessible(true);
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注解里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        cs.setString(procedureParamType.paramindex(), (String)field.get(param));
                                        break;
                                    case OracleTypes.DATE:
                                        cs.setDate(procedureParamType.paramindex(), utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                /////为Out的时候
                                if (procedureParamType.Oracletype() == OracleTypes.CURSOR) {
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.CURSOR);
                                    Map.Entry<Class,Integer> map1=new DefaultMapEntry(procedureParamType.T_CLASS(),procedureParamType.paramindex());
                                    mapMap.put(field.getName(),map1);
                                    //map.put("records",procedureParamType.paramindex());////记录参数的序号 以便返回
                                }
                            }
                        }
                        cs.execute();
                        Map<String, List<?>> map1=new HashMap<>();
                        for(String item:mapMap.keySet()){
                            ResultSet resultSet=(ResultSet) cs.getObject(mapMap.get(item).getValue());
                            List<?> list=resultset2List(mapMap.get(item).getKey(),resultSet);
                            map1.put(item,list);
                        }
                        return map1;
                    }
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.toString());
                } catch (InvocationTargetException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (SecurityException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (NoSuchMethodException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (InstantiationException ex) {
                    throw new SQLDataException(ex.toString());
                }
                return null;
            }
        });
        return map;
    }


    ///返回多个MAP
    public static Map<String, List<Map<String, Object>>> execProcdureReturnMResultSet(String procsql, final ProcedureParam param, JdbcTemplate jdbcTemplate) {
        Map<String, List<Map<String, Object>>> map = jdbcTemplate.execute(procsql, new CallableStatementCallback<Map<String, List<Map<String, Object>>>>() {
            @Override
            public Map<String, List<Map<String, Object>>> doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                Map<String, Map.Entry<Class, Integer>> mapMap = new HashMap<>();
                try {
                    Field[] fields = param.getClass().getDeclaredFields();

                    if (fields != null && fields.length > 0) {
                        for (Field field : fields) {
                            ProcedureParamType procedureParamType = field.getAnnotation(ProcedureParamType.class);///获取属性上的注释
                            if (procedureParamType == null) continue;///无注解的忽视
                            field.setAccessible(true);
                            if (procedureParamType.InOut().equals(InOutTypeEnum.intype)) {////注解里的参数类型为IN
                                switch (procedureParamType.Oracletype()) {
                                    case OracleTypes.VARCHAR:
                                        cs.setString(procedureParamType.paramindex(), (String)field.get(param));
                                        break;
                                    case OracleTypes.DATE:
                                        cs.setDate(procedureParamType.paramindex(), utilDate2sqlDate((java.util.Date) field.get(param)));
                                        break;
                                    case OracleTypes.NUMBER:
                                        cs.setInt(procedureParamType.paramindex(), (int) field.get(param));
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                /////为Out的时候
                                if (procedureParamType.Oracletype() == OracleTypes.CURSOR) {
                                    cs.registerOutParameter(procedureParamType.paramindex(), OracleTypes.CURSOR);
                                    Map.Entry<Class,Integer> map1=new DefaultMapEntry(procedureParamType.T_CLASS(),procedureParamType.paramindex());
                                    mapMap.put(field.getName(),map1);
                                    //map.put("records",procedureParamType.paramindex());////记录参数的序号 以便返回
                                }
                            }
                        }
                        cs.execute();
                        Map<String, List<Map<String, Object>>> map1=new HashMap<>();
                        for(String item:mapMap.keySet()){
                            ResultSet resultSet=(ResultSet) cs.getObject(mapMap.get(item).getValue());
                            ResultSetMetaData md = resultSet.getMetaData();
                            int columnCount = md.getColumnCount();
                            List<Map<String,Object>> list=new ArrayList<>();
                            while (resultSet.next()) {
                                Map<String,Object> rowData = new HashMap<String,Object>();
                                for (int i = 1; i <= columnCount; i++) {
                                    rowData.put(md.getColumnName(i), resultSet.getObject(i));
                                }
                                list.add(rowData);
                            }
                            map1.put(item,list);
                        }
                        return map1;
                    }
                } catch (SQLException ex) {
                    throw ex;
                } catch (IllegalAccessException ex) {
                    throw new SQLDataException(ex.toString());
                } catch (IllegalArgumentException ex) {
                    throw new SQLException(ex.toString());

                } catch (SecurityException ex) {
                    throw new SQLDataException(ex.toString());
                }
                return null;
            }
        });
        return map;
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

    public static  <T> List<T> resultset2List(Class<T>tClass, ResultSet resultSet) throws SQLException,IllegalArgumentException,IllegalAccessException,InvocationTargetException,SecurityException,NoSuchMethodException,InstantiationException {
        ResultSetMetaData resultSetMetaData=null;
        String temp="";
        T t=null;
        List<T> list=new ArrayList<>();
        Method s=null;
        boolean isHasValue=false;
        resultSetMetaData=resultSet.getMetaData();
        while (resultSet!=null&&resultSet.next()){
            t=tClass.newInstance();
            if(tClass.getName().equals("java.lang.String")){
                isHasValue = true;
                temp = resultSetMetaData.getColumnLabel(1);
                t=(T)resultSet.getObject(temp);
            }else {
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    temp = resultSetMetaData.getColumnLabel(i);
                    String temp1 = temp.replaceAll("_", "").toLowerCase();
                    for (Field filed : tClass.getDeclaredFields()) {
                        String filedname = filed.getName();
                        if (filedname.equalsIgnoreCase(temp1)) {
                            isHasValue = true;

                            s = tClass.getDeclaredMethod(makeSetMethodName(filed.getName()), filed.getType());
                            s.invoke(t, resultSet.getObject(temp));
                            break;///匹配到跳出当前循环
                        }
                    }
                }
            }
            if(isHasValue) list.add(t);
        }
        return list;
    }

    public static <T> T result2Object(Class<T> tClass,ResultSet resultSet) throws SQLException,IllegalArgumentException,IllegalAccessException,InvocationTargetException,SecurityException,NoSuchMethodException,InstantiationException{
        List<T> list= Optional.ofNullable(resultset2List(tClass,resultSet)).orElseThrow(() -> new RuntimeException("查询结果为空！"));
        return list==null?null:list.get(0);
    }


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

    public static java.sql.Date utilDate2sqlDate(java.util.Date date){
        return date==null?null:new java.sql.Date(date.getTime());
    }
}
