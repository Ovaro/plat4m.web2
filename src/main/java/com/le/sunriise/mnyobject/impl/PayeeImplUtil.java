package com.le.sunriise.mnyobject.impl;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import com.le.sunriise.mnyobject.Payee;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PayeeImplUtil {

    private static final String TABLE_NAME = "PAY";
    private static final String COL_ID = "hpay";
    private static final String COL_NAME = "szFull";
    private static final String COL_PARENT_ID = "hpayParent";

    public static Map<Integer, Payee> getPayees(Database db) throws IOException {
        Map<Integer, Payee> payees = new HashMap<Integer, Payee>();

        String tableName = TABLE_NAME;
        Table table = db.getTable(tableName);
        Cursor cursor = null;
        try {
            cursor = Cursor.createCursor(table);

            while (cursor.moveToNextRow()) {
                Map<String, Object> row = cursor.getCurrentRow();

                String name = (String) row.get(COL_NAME);
                if (name == null) {
                    continue;
                }
                if (name.length() == 0) {
                    continue;
                }
                PayeeImpl payee = new PayeeImpl();
                payee.setName(name);

                Integer hpay = (Integer) row.get(COL_ID);
                payee.setId(hpay);

                Integer hpayParent = (Integer) row.get(COL_PARENT_ID);
                payee.setParent(hpayParent);

                Date serialDate = (Date) row.get("dtSerial");
                payee.setSerialDate(serialDate);

                // sguid
                String sguid = (String) row.get("sguid");
                payee.setGuid(sguid);

                // sguid
                Boolean b = (Boolean) row.get("fHidden");
                payee.setHidden(b);

                payees.put(hpay, payee);
            }
        } finally {}
        return payees;
    }
}
