package com.nico.test;


import com.nico.common.utils.ProcedureUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class test {

    private final String procName="{call P_GET_DEALER_OPERATOR(?,?)}";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public List<Dealer> getDealerOperator(ProcedureParamTest param)throws Exception{

        List<Dealer>  result= (List<Dealer>) ProcedureUtil.execProcedureReturenTList(procName,param,jdbcTemplate);

        return  result;
    }
}
