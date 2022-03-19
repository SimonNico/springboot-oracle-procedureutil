# springboot-oracle-procedureutil

##### Specify the index of parameter which it's been uesed in calling procedure, same parameter sequence as the procedure u call

```Java
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
```
##### And ```OracleType``` is the procedure return data type,```T_CLASS``` is the type u wanna mapping,the fild name of ```T_CLASS``` should be same as procedure's returns




At last call the method like this:

```Java
private final String procName="{call P_GET_DEALER_OPERATOR(?,?)}";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public List<Dealer> getDealerOperator()throws Exception{
        ProcedureParamTest param=new ProcedureParamTest();
        param.setName("nico");
        List<Dealer>  result= (List<Dealer>) ProcedureUtil.execProcedureReturenTList(procName,param,jdbcTemplate);

        return  result;
    }
