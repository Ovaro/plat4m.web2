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

public interface Security {
    public abstract Integer getId();

    public abstract void setId(Integer id);

    public abstract String getName();

    public abstract void setName(String name);

    public abstract String getSymbol();

    public abstract void setSymbol(String symbol);

    public abstract Date getSerialDate();

    public abstract void setSerialDate(Date serialDate);

    public abstract String getGuid();

    public abstract void setGuid(String guid);

    public abstract String getComment();

    public abstract void setComment(String comment);

    public abstract String getExchangeId();

    public abstract void setExchangeId(String exchangeId);

    public abstract String getCurrencyId();

    public abstract void setCurrencyId(String currencyId);

    public abstract String getCountryId();

    public abstract void setCountryId(String countryId);

    public abstract Integer getLinkId();

    public abstract void setLinkId(Integer id);

    public abstract Integer getType();

    public abstract void setType(Integer id);
}
