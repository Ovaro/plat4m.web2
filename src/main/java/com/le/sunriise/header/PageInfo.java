package com.le.sunriise.header;

import com.healthmarketscience.jackcess.PageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageInfo {

    private static final Logger log = LoggerFactory.getLogger(PageInfo.class);

    private int pageNumber;

    private byte pageType;

    public PageInfo(int pageNumber, byte pageType) {
        this.pageNumber = pageNumber;
        this.pageType = pageType;
    }

    public static String pageTypeToString(byte pageType) {
        switch (pageType) {
            case PageTypes.DATA:
                return "PageTypes.DATA";
            case PageTypes.INDEX_LEAF:
                return "PageTypes.INDEX_LEAF";
            case PageTypes.INDEX_NODE:
                return "PageTypes.INDEX_NODE";
            case PageTypes.INVALID:
                return "PageTypes.INVALID";
            case PageTypes.TABLE_DEF:
                return "PageTypes.TABLE_DEF";
            case PageTypes.USAGE_MAP:
                return "PageTypes.USAGE_MAP";
            default:
                return "PageTypes.UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return pageNumber + ", " + pageTypeToString(pageType);
    }
}
