package com.le.sunriise.mnyobject.impl;

import com.le.sunriise.mnyobject.TransferLink;

public class TransferLinkImpl implements TransferLink {

    private Integer fromId;
    private Integer linkId;

    @Override
    public Integer getFromId() {
        return fromId;
    }

    @Override
    public void setFromId(Integer fromId) {
        this.fromId = fromId;
    }

    @Override
    public Integer getLinkId() {
        return linkId;
    }

    @Override
    public void setLinkId(Integer linkId) {
        this.linkId = linkId;
    }
}
