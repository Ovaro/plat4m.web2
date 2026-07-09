/*******************************************************************************
 * Copyright (c) 2010 Hung Le
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *******************************************************************************/
package com.le.sunriise.mnyobject.impl;

import com.le.sunriise.mnyobject.Lot;
import com.le.sunriise.mnyobject.MnyObject;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LotImpl extends MnyObject implements Comparable<LotImpl>, Lot {

    private static final Logger log = LoggerFactory.getLogger(LotImpl.class);

    private Integer id;
    private Integer buyTrnId;
    private Integer sellTrnId;
    private Double quantity;
    private Integer accountId;
    private Integer securityId;
    private Date buyDate;
    private Date sellDate;
    private Integer lotOpenId;
    private Integer lott;
    private Integer openTrnId;
    private Integer closeTrnId;
    private Date openDate;
    private Date closeDate;
    private String guid;
    private Date serialDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBuyTrnId() {
        return buyTrnId;
    }

    public void setBuyTrnId(Integer buyTrnId) {
        this.buyTrnId = buyTrnId;
    }

    public Integer getSellTrnId() {
        return sellTrnId;
    }

    public void setSellTrnId(Integer sellTrnId) {
        this.sellTrnId = sellTrnId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getSecurityId() {
        return securityId;
    }

    public void setSecurityId(Integer securityId) {
        this.securityId = securityId;
    }

    public Date getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(Date buyDate) {
        this.buyDate = buyDate;
    }

    public Date getSellDate() {
        return sellDate;
    }

    public void setSellDate(Date sellDate) {
        this.sellDate = sellDate;
    }

    public Integer getLotOpenId() {
        return lotOpenId;
    }

    public void setLotOpenId(Integer lotOpenId) {
        this.lotOpenId = lotOpenId;
    }

    public Integer getLott() {
        return lott;
    }

    public void setLott(Integer lott) {
        this.lott = lott;
    }

    public Integer getOpenTrnId() {
        return openTrnId;
    }

    public void setOpenTrnId(Integer openTrnId) {
        this.openTrnId = openTrnId;
    }

    public Integer getCloseTrnId() {
        return closeTrnId;
    }

    public void setCloseTrnId(Integer closeTrnId) {
        this.closeTrnId = closeTrnId;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public Date getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(Date closeDate) {
        this.closeDate = closeDate;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public int compareTo(LotImpl o) {
        if (o == null) {
            return 1;
        }
        if (id == null) {
            return o.getId() == null ? 0 : -1;
        }
        if (o.getId() == null) {
            return 1;
        }
        return id.compareTo(o.getId());
    }

    public Date getSerialDate() {
        return serialDate;
    }

    public void setSerialDate(Date serialDate) {
        this.serialDate = serialDate;
    }
}
