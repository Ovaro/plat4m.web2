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
package com.le.sunriise.password.dict;

import com.le.sunriise.password.PasswordUtils;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckPasswordWorker implements Callable<String> {

    private static final Logger log = LoggerFactory.getLogger(CheckPasswordWorker.class);

    private final PasswordWorkerContext context;

    private final String testPassword;

    private boolean doubleCheck = true;

    public CheckPasswordWorker(PasswordWorkerContext context, String testPassword) {
        super();
        this.context = context;
        this.testPassword = testPassword;
    }

    @Override
    public String call() throws Exception {
        long counterValue = this.context.getCounter().incrementAndGet();
        int max = 100000;
        if ((max > 0) && ((counterValue % max) == 0)) {
            log.info("Have checked " + counterValue);
        }

        if (checkPassword(testPassword)) {
            log.info("testPassword=" + testPassword + ", YES");
            context.getResultCollector().setResult(testPassword);
            context.getQuit().getAndSet(true);
            return testPassword;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("testPassword=" + testPassword + ", NO");
            }
            return null;
        }
    }

    private boolean checkPassword(String testPassword) throws IOException {
        boolean result = false;
        boolean checkUsingOpenDb = (context.getHeaderPage() == null);

        if (checkUsingOpenDb) {
            result = PasswordUtils.checkUsingOpenDb(context.getDbFile(), testPassword);
        } else {
            result = PasswordUtils.checkUsingHeaderPage(context.getHeaderPage(), testPassword);
            if (result) {
                if (doubleCheck) {
                    result = PasswordUtils.doubleCheck(context.getHeaderPage(), testPassword);
                }
            }
        }

        return result;
    }

    public PasswordWorkerContext getContext() {
        return context;
    }
}
