package com.le.sunriise.mnyobject.impl;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import com.le.sunriise.mnyobject.Security;
import com.le.sunriise.mnyobject.SecurityPrice;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SecurityImplUtil {

    private static final String COL_SYMBOL = "szSymbol";
    private static final String COL_NAME = "szFull";
    private static final String COL_ID = "hsec";
    private static final String TABLE_NAME = "SEC";

    public static Map<Integer, Security> getSecurities(Database db) throws IOException {
        Map<Integer, Security> securities = new HashMap<Integer, Security>();

        String tableName = TABLE_NAME;
        Table table = db.getTable(tableName);
        Cursor cursor = null;
        try {
            cursor = Cursor.createCursor(table);

            while (cursor.moveToNextRow()) {
                Map<String, Object> row = cursor.getCurrentRow();

                addSecurity(row, securities);
            }
        } finally {
        }

        return securities;
    }

    private static void addSecurity(Map<String, Object> row, Map<Integer, Security> securities) {
        SecurityImpl security = new SecurityImpl();

        Integer hsec = (Integer) row.get(COL_ID);
        security.setId(hsec);

        String szFull = (String) row.get(COL_NAME);
        security.setName(szFull);

        String szSymbol = (String) row.get(COL_SYMBOL);
        security.setSymbol(szSymbol);

        Date serialDate = (Date) row.get("dtSerial");
        security.setSerialDate(serialDate);

        String guid = (String) row.get("sguid");
        security.setGuid(guid);

        String c = (String) row.get("mComment");
        security.setComment(c);

        String e = (String) row.get("szExchg");
        security.setExchangeId(e);

        Integer curn = (Integer) row.get("hcrnc");
        if (curn != null) security.setCurrencyId(curn.toString());

        Integer country = (Integer) row.get("hcntry");
        if (country != null) security.setCountryId(country.toString());

        Integer secLink = (Integer) row.get("hsecLink");
        if (secLink != null) security.setLinkId(secLink);

        Integer secType = (Integer) row.get("sct");
        if (secType != null) security.setType(secType);

        securities.put(hsec, security);
    }

    public static SecurityPrice createSPTransactionFromRow(Database db, Map<String, Object> row, Instant onlyIfUpdatedAfter)
        throws IOException {
        Date serialDate = (Date) row.get("dtSerial");
        if (onlyIfUpdatedAfter != null) {
            if (!serialDate.toInstant().isAfter(onlyIfUpdatedAfter)) {
                return null;
            }
        }

        SecurityPrice sp = new SecurityPriceImpl();
        sp.setSerialDate(serialDate);

        Integer hsp = (Integer) row.get("hsp");
        sp.setId(hsp);

        // date
        Date date = (Date) row.get("dt");
        sp.setDate(date);

        Integer sec = (Integer) row.get("hsec");
        sp.setSecurityId(sec);

        // amount
        Double price = (Double) row.get("dPrice");
        sp.setPrice(price);

        Double open = (Double) row.get("dOpen");
        sp.setOpen(open);

        Double close = (Double) row.get("dClose");
        sp.setClose(close);

        Double high = (Double) row.get("dHigh");
        sp.setHigh(high);

        Double low = (Double) row.get("dLow");
        sp.setLow(low);

        Integer vol = (Integer) row.get("vol");
        sp.setVolume(vol);

        return sp;
    }
}
