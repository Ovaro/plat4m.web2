package com.le.sunriise.scan;

import com.le.sunriise.header.HeaderPage;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableDefPage {

    private static final Logger log = LoggerFactory.getLogger(TableDefPage.class);

    private int previousPageNumber = 0;

    private final int pageNumber;

    private final int nextPageNumber;

    private final HeaderPage headerPage;

    public TableDefPage(int pageNumber, ByteBuffer buffer, HeaderPage headerPage) {
        this.pageNumber = pageNumber;
        this.headerPage = headerPage;

        this.nextPageNumber = buffer.getInt(headerPage.getJetFormat().OFFSET_NEXT_TABLE_DEF_PAGE);
        // log.info("TDEF " + pageNumber + " -> " + nextPageNumber);
    }

    public int getPreviousPageNumber() {
        return previousPageNumber;
    }

    public void setPreviousPageNumber(int previousPageNumber) {
        this.previousPageNumber = previousPageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getNextPageNumber() {
        return nextPageNumber;
    }

    public HeaderPage getHeaderPage() {
        return headerPage;
    }

    public boolean isParent() {
        return previousPageNumber <= 0;
    }

    public boolean isChild() {
        return !isParent();
    }
}
