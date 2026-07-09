/*******************************************************************************
 * Copyright (c) 2013 Hung Le
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

package com.le.sunriise.mnyobject;

import java.util.Date;

public interface Lot {
    public abstract Integer getId();

    public abstract void setId(Integer id);

    public abstract Integer getBuyTrnId();

    public abstract void setBuyTrnId(Integer id);

    public abstract Integer getSellTrnId();

    public abstract void setSellTrnId(Integer id);

    public abstract Double getQuantity();

    public abstract void setQuantity(Double quantity);

    public abstract void setAccountId(Integer accountId);

    public abstract Integer getAccountId();

    public abstract Integer getSecurityId();

    public abstract void setSecurityId(Integer securityId);

    public abstract Date getBuyDate();

    public abstract void setBuyDate(Date date);

    public abstract Date getSellDate();

    public abstract void setSellDate(Date date);

    public abstract Integer getLotOpenId();

    public abstract void setLotOpenId(Integer id);

    public abstract Integer getLott();

    public abstract void setLott(Integer lott);

    public abstract Integer getOpenTrnId();

    public abstract void setOpenTrnId(Integer id);

    public abstract Integer getCloseTrnId();

    public abstract void setCloseTrnId(Integer id);

    public abstract Date getOpenDate();

    public abstract void setOpenDate(Date date);

    public abstract Date getCloseDate();

    public abstract void setCloseDate(Date date);

    public abstract String getGuid();

    public abstract void setGuid(String guid);

    public abstract Date getSerialDate();

    public abstract void setSerialDate(Date date);
}
