package com.le.sunriise.mnyobject.impl;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import com.le.sunriise.mnyobject.TransferLink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransferLinkImplUtil {

    public static final String TABLE_NAME = "TRN_XFER";
    private static final String COL_FROM_ID = "htrnFrom";
    private static final String COL_LINK_ID = "htrnLink";

    public static List<TransferLink> getTransferLinks(Database db) throws IOException {
        List<TransferLink> transferLinks = new ArrayList<>();
        Table table = db.getTable(TABLE_NAME);
        if (table == null) {
            return transferLinks;
        }

        Cursor cursor = Cursor.createCursor(table);
        while (cursor.moveToNextRow()) {
            Map<String, Object> row = cursor.getCurrentRow();
            Integer fromId = (Integer) row.get(COL_FROM_ID);
            Integer linkId = (Integer) row.get(COL_LINK_ID);
            if (fromId == null || linkId == null) {
                continue;
            }

            TransferLink transferLink = new TransferLinkImpl();
            transferLink.setFromId(fromId);
            transferLink.setLinkId(linkId);
            transferLinks.add(transferLink);
        }

        return transferLinks;
    }
}
