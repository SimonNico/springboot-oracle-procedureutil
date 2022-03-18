package com.nico.test;

import com.nico.common.dal.InOutTypeEnum;
import com.nico.common.dal.ProcedureParam;
import com.nico.common.dal.ProcedureParamType;
import oracle.jdbc.internal.OracleTypes;

import java.io.Serializable;

public class ProcedureParamTest implements Serializable, ProcedureParam {

    private static final long serialVersionUID = 1398154527710378274L;

    @ProcedureParamType(paramindex = 1)
    private String name;

    @ProcedureParamType(paramindex = 2,InOut = InOutTypeEnum.outtype,Oracletype = OracleTypes.CURSOR,T_CLASS = Dealer.class)
    private String OData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOData() {
        return OData;
    }

    public void setOData(String OData) {
        this.OData = OData;
    }
}
