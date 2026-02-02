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

import java.math.BigDecimal;
import java.util.Date;

//JK
public interface SecurityPrice {
    public abstract Integer getId();

    public abstract void setId(Integer id);

    public abstract Date getDate();

    public abstract void setDate(Date date);

    public abstract Integer getSecurityId();

    public abstract void setSecurityId(Integer id);

    public abstract Double getPrice();

    public abstract void setPrice(Double price);

    public abstract Double getOpen();

    public abstract void setOpen(Double price);

    public abstract Double getClose();

    public abstract void setClose(Double price);

    public abstract Double getHigh();

    public abstract void setHigh(Double price);

    public abstract Double getLow();

    public abstract void setLow(Double price);

    public abstract Integer getVolume();

    public abstract void setVolume(Integer id);

    public abstract Double getChange();

    public abstract void setChange(Double change);

    public abstract Date getSerialDate();

    public abstract void setSerialDate(Date serialDate);
}
