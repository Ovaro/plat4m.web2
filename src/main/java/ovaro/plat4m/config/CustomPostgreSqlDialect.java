package ovaro.plat4m.config;

import java.sql.Types;
import org.hibernate.dialect.PostgreSQLDialect;

public class CustomPostgreSqlDialect extends PostgreSQLDialect {

    // @Override
    // public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor)
    // {

    //     switch (sqlTypeDescriptor.getSqlType())
    //     {
    //     // case Types.CLOB:
    //     //     return VarcharTypeDescriptor.INSTANCE;
    //     // case Types.BLOB:
    //     //     return VarcharTypeDescriptor.INSTANCE;
    //     case 1111://1111 should be json of pgsql
    //         return VarcharTypeDescriptor.INSTANCE;
    //     }
    //     return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
    // }
    public CustomPostgreSqlDialect() {
        super();
        System.out.println("ZZZ-CustomPostgreSqlDialect");
        //registerColumnTypes(1111, "string");
    }
}
