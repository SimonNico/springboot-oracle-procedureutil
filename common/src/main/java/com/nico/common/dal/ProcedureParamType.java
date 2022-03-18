package com.nico.common.dal;

import com.nico.common.dal.InOutTypeEnum;
import oracle.jdbc.internal.OracleTypes;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ProcedureParamType {

    ///参数位置
    int paramindex();

    ///输入输出参数
    InOutTypeEnum InOut() default InOutTypeEnum.intype;

    ///返回类型 如OracleTypes.CURSOR
    int Oracletype() default OracleTypes.VARCHAR;

    ////返回值的类 用于多个结果集
    Class<?> T_CLASS() default void.class;
}
