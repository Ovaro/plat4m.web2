package com.le.sunriise.mnyobject.impl;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import com.le.sunriise.mnyobject.Lot;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LotImplUtil {

    private static final String TABLE_NAME = "LOT";
    private static final String COL_ID = "hlot";
    private static final String COL_TRN_ID_BUY = "htrnBuy";
    private static final String COL_TRN_ID_SELL = "htrnSell";
    private static final String COL_QTY = "qty";
    private static final String COL_ACCT_ID = "hacct";
    private static final String COL_SEC_ID = "hsec";
    private static final String COL_DATE_BUY = "dtBuy";
    private static final String COL_DATE_SELL = "dtSell";
    private static final String COL_LOT_OPEN_ID = "hlotOpen";
    private static final String COL_LOTT = "lott";
    private static final String COL_TRN_ID_OPEN = "htrnOpen";
    private static final String COL_TRN_ID_CLOSE = "htrnClose";
    private static final String COL_DATE_OPEN = "dtOpen";
    private static final String COL_DATE_CLOSE = "dtClose";
    private static final String COL_GUID = "sguid";
    private static final String COL_SERIAL_DATE = "dtSerial";

    public static Map<Integer, Lot> getLots(Database db) throws IOException {
        Map<Integer, Lot> lots = new HashMap<Integer, Lot>();

        String tableName = TABLE_NAME;
        Table table = db.getTable(tableName);
        Cursor cursor = null;
        try {
            cursor = Cursor.createCursor(table);

            while (cursor.moveToNextRow()) {
                Map<String, Object> row = cursor.getCurrentRow();

                LotImpl lot = new LotImpl();

                Integer hlot = (Integer) row.get(COL_ID);
                lot.setId(hlot);

                Integer htrnBuy = (Integer) row.get(COL_TRN_ID_BUY);
                lot.setBuyTrnId(htrnBuy);

                Integer htrnSell = (Integer) row.get(COL_TRN_ID_SELL);
                lot.setSellTrnId(htrnSell);

                Double qty = (Double) row.get(COL_QTY);
                lot.setQuantity(qty);

                Integer hacct = (Integer) row.get(COL_ACCT_ID);
                lot.setAccountId(hacct);

                Integer hsec = (Integer) row.get(COL_SEC_ID);
                lot.setSecurityId(hsec);

                Date dtBuy = (Date) row.get(COL_DATE_BUY);
                lot.setBuyDate(dtBuy);

                Date dtSell = (Date) row.get(COL_DATE_SELL);
                lot.setSellDate(dtSell);

                Integer hlotOpen = (Integer) row.get(COL_LOT_OPEN_ID);
                lot.setLotOpenId(hlotOpen);

                Integer lott = (Integer) row.get(COL_LOTT);
                lot.setLott(lott);

                Integer htrnOpen = (Integer) row.get(COL_TRN_ID_OPEN);
                lot.setOpenTrnId(htrnOpen);

                Integer htrnClose = (Integer) row.get(COL_TRN_ID_CLOSE);
                lot.setCloseTrnId(htrnClose);

                Date dtOpen = (Date) row.get(COL_DATE_OPEN);
                lot.setOpenDate(dtOpen);

                Date dtClose = (Date) row.get(COL_DATE_CLOSE);
                lot.setCloseDate(dtClose);

                String sguid = (String) row.get(COL_GUID);
                lot.setGuid(sguid);

                Date serialDate = (Date) row.get(COL_SERIAL_DATE);
                lot.setSerialDate(serialDate);

                lots.put(hlot, lot);
            }
        } finally {
        }
        return lots;
    }
}
