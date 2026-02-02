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

import com.le.sunriise.mnyobject.MnyObject;
import com.le.sunriise.mnyobject.Payee;
import java.util.Date;

public class PayeeImpl extends MnyObject implements Comparable<PayeeImpl>, Payee {

    private Integer id;

    private Integer parent;

    private String name;

    private Date serialDate;

    private String guid;

    private Boolean hidden;

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Payee#getId()
     */
    @Override
    public Integer getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Payee#setId(java.lang.Integer)
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Payee#getParent()
     */
    @Override
    public Integer getParent() {
        return parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Payee#setParent(java.lang.Integer)
     */
    @Override
    public void setParent(Integer parent) {
        this.parent = parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Payee#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.Payee#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(PayeeImpl o) {
        return id.compareTo(o.getId());
    }

    public Date getSerialDate() {
        return serialDate;
    }

    public void setSerialDate(Date serialDate) {
        this.serialDate = serialDate;
    }
}
