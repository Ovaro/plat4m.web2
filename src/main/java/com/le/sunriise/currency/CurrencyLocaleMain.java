package com.le.sunriise.currency;

import java.text.NumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyLocaleMain {

    private static final Logger log = LoggerFactory.getLogger(CurrencyLocaleMain.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        Double amount = new Double(80.700000);

        NumberFormat numberFormat = null;

        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            numberFormat = NumberFormat.getNumberInstance(locale);
            log.info(locale + ", " + numberFormat.format(amount));
        }
    }
}
