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
package com.le.sunriise.mnyobject;

import java.util.Date;

public interface FinancialInstitution {
    /**
     * Get the internal MsMoney id for this account.
     *
     * @return an non-negative Integer which is the internal MsMoney id for this
     *         account.
     */
    public abstract Integer getId();

    /**
     * Set the internal MsMoney id for this account.
     *
     * @param id
     *            an non-negative Integer which is the internal MsMoney id for
     *            this account.
     */
    public abstract void setId(Integer id);

    /**
     * Get the account name.
     *
     * @return
     */
    public abstract String getName();

    /**
     * Set the account name.
     *
     * @param name
     */
    public abstract void setName(String name);

    public abstract String getComment();

    public abstract void setComment(String comment);

    public abstract Date getSerialDate();

    public abstract void setSerialDate(Date serialDate);
}
