package com.le.sunriise.mnyobject.impl;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import com.le.sunriise.mnyobject.FinancialInstitution;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FinancialInstitutionImplUtil {

    private static final String TABLE_NAME = "FI";
    private static final String COL_ID = "hfi";
    private static final String COL_NAME = "szFull";
    private static final String COL_COMMENT = "mComment";
    private static final String COL_DT_SERIAL = "dtSerial";

    public static Map<Integer, FinancialInstitution> getFinancialInstitutions(Database db) throws IOException {
        Map<Integer, FinancialInstitution> fis = new HashMap<Integer, FinancialInstitution>();

        String tableName = TABLE_NAME;
        Table table = db.getTable(tableName);
        Cursor cursor = Cursor.createCursor(table);

        while (cursor.moveToNextRow()) {
            Map<String, Object> row = cursor.getCurrentRow();
            FinancialInstitution fi = new FinancialInstitutionImpl();
            Integer hfi = (Integer) row.get(COL_ID);
            fi.setId(hfi);

            String name = (String) row.get(COL_NAME);
            fi.setName(name);

            String comment = (String) row.get(COL_COMMENT);
            fi.setComment(comment);

            Date serialDate = (Date) row.get(COL_DT_SERIAL);
            fi.setSerialDate(serialDate);
            fis.put(hfi, fi);
        }

        return fis;
    }
}
