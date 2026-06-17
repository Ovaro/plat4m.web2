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

import com.le.sunriise.mnyobject.TransactionInfo;

public class TransactionInfoImpl implements TransactionInfo {

    // TRN.grftt bits
    private Integer flag = 0;

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#getFlag()
     */
    @Override
    public Integer getFlag() {
        return flag;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#setFlag(java.lang.Integer)
     */
    @Override
    public void setFlag(Integer flag) {
        if (flag == null) {
            flag = 0;
        }
        this.flag = flag;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#isTransfer()
     */
    @Override
    public boolean isTransfer() {
        // bit 1 == transfer
        int mask = (1 << 1);
        return (flag & mask) == mask;
    }

    public boolean isBit3() {
        // bit 1 == transfer
        int mask = (1 << 3);
        return (flag & mask) == mask;
    }

    public boolean isBit7() {
        // bit 1 == transfer
        int mask = (1 << 7);
        return (flag & mask) == mask;
    }

    public boolean isBit9() {
        // bit 1 == transfer
        int mask = (1 << 9);
        return (flag & mask) == mask;
    }

    public boolean isBit10() {
        // bit 1 == transfer
        int mask = (1 << 10);
        return (flag & mask) == mask;
    }

    public boolean isBit11() {
        // bit 1 == transfer
        int mask = (1 << 11);
        return (flag & mask) == mask;
    }

    public boolean isBit12() {
        // bit 1 == transfer
        int mask = (1 << 12);
        return (flag & mask) == mask;
    }

    public boolean isBit13() {
        // bit 1 == transfer
        int mask = (1 << 13);
        return (flag & mask) == mask;
    }

    public boolean isBit14() {
        // bit 1 == transfer
        int mask = (1 << 14);
        return (flag & mask) == mask;
    }

    public boolean isBit15() {
        // bit 1 == transfer
        int mask = (1 << 15);
        return (flag & mask) == mask;
    }

    public boolean isBit16() {
        // bit 1 == transfer
        int mask = (1 << 16);
        return (flag & mask) == mask;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#isTransferTo()
     */
    @Override
    public boolean isTransferTo() {
        // bit 2 == transfer to
        int mask = (1 << 2);
        return (flag & mask) == mask;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#isInvestment()
     */
    @Override
    public boolean isInvestment() {
        // bit 4 == investment trn (need to figure out how to tell what
        // kind--other grftt bits?)
        int mask = (1 << 4);
        return (flag & mask) == mask;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#isSplitParent()
     */
    @Override
    public boolean isSplitParent() {
        // bit 5 == split parent
        int mask = (1 << 5);
        return (flag & mask) == mask;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#isSplitChild()
     */
    @Override
    public boolean isSplitChild() {
        // bit 6 == split child
        int mask = (1 << 6);
        return (flag & mask) == mask;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.le.sunriise.mnyobject.TransactionInfo#isVoid()
     */
    @Override
    public boolean isVoid() {
        // bit 8 == void"
        int mask = (1 << 8);
        return (flag & mask) == mask;
    }

    @Override
    public String toString() {
        return (
            "TransactionInfoImpl [flag=" +
            flag +
            ", getFlag()=" +
            getFlag() +
            ", isTransfer()=" +
            isTransfer() +
            ", isBit3()=" +
            isBit3() +
            ", isBit7()=" +
            isBit7() +
            ", isBit9()=" +
            isBit9() +
            ", isBit10()=" +
            isBit10() +
            ", isBit11()=" +
            isBit11() +
            ", isBit12()=" +
            isBit12() +
            ", isBit13()=" +
            isBit13() +
            ", isBit14()=" +
            isBit14() +
            ", isBit15()=" +
            isBit15() +
            ", isBit16()=" +
            isBit16() +
            ", isTransferTo()=" +
            isTransferTo() +
            ", isInvestment()=" +
            isInvestment() +
            ", isSplitParent()=" +
            isSplitParent() +
            ", isSplitChild()=" +
            isSplitChild() +
            ", isVoid()=" +
            isVoid() +
            "]"
        );
    }

    public static void main(String[] args) {
        TransactionInfoImpl transactionInfo = new TransactionInfoImpl();
        transactionInfo.setFlag(2097184);
        System.out.println(transactionInfo.toString());
    }
}
