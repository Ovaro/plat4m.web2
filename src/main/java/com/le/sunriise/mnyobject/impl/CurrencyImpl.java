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

import com.le.sunriise.mnyobject.Currency;
import com.le.sunriise.mnyobject.MnyObject;
import java.util.Date;

public class CurrencyImpl extends MnyObject implements Comparable<CurrencyImpl>, Currency {

    private Integer id;

    private String name;

    private String isoCode;

    private Date serialDate;

    private String guid;

    @Override
    public int compareTo(CurrencyImpl o) {
        return id.compareTo(o.getId());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Currency#getId()
     */
    @Override
    public Integer getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Currency#setId(java.lang.Integer)
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Currency#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Currency#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Currency#getIsoCode()
     */
    @Override
    public String getIsoCode() {
        return isoCode;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Currency#setIsoCode(java.lang.String)
     */
    @Override
    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public Date getSerialDate() {
        return serialDate;
    }

    public void setSerialDate(Date serialDate) {
        this.serialDate = serialDate;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }
}
