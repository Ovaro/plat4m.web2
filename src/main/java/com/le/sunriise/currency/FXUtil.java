package com.le.sunriise.currency;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FXUtil {

    public static List<FX> getFX(Database db) throws IOException {
        List<FX> entries = new ArrayList<FX>();
        // CRNC
        // (PK) CRNC.hcrnc
        //
        // CRNC_EXCHG
        // (FK) CRNC_EXCHG.hcrncFrom -> CRNC.hcrnc
        // (FK) CRNC_EXCHG.hcrncTo -> CRNC.hcrnc
        Table tCRNC = db.getTable("CRNC");
        Table tCRNC_EXCHG = db.getTable("CRNC_EXCHG");
        Cursor cCRNC_EXCHG = null;
        Cursor cCRNC = null;
        try {
            cCRNC_EXCHG = Cursor.createCursor(tCRNC_EXCHG);
            cCRNC = Cursor.createCursor(tCRNC);

            // {hcrncFrom=1, hcrncTo=25, rate=1.00969, dt=Mon Feb 28 00:00:00
            // PST 10000, fReversed=false, fThroughEuro=false, exchgid=-1,
            // fHist=false, szSymbol=null}
            Map<String, Object> rCRNC_EXCHG = null;
            while ((rCRNC_EXCHG = cCRNC_EXCHG.getNextRow()) != null) {
                Integer hcrncFrom = (Integer) rCRNC_EXCHG.get("hcrncFrom");
                Integer hcrncTo = (Integer) rCRNC_EXCHG.get("hcrncTo");
                Double rate = (Double) rCRNC_EXCHG.get("rate");
                Date dt = (Date) rCRNC_EXCHG.get("dt");

                entries.add(new FX(hcrncFrom, hcrncTo, rate, dt));
                // log.info(crncRow);
            }
        } finally {
            if (cCRNC_EXCHG != null) {
                cCRNC_EXCHG = null;
            }

            if (cCRNC != null) {
                cCRNC = null;
            }
        }

        return entries;
    }
}
