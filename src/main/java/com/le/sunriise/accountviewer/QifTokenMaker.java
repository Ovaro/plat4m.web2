package com.le.sunriise.accountviewer;

import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class QifTokenMaker extends PlainTextTokenMaker {

    private static final Logger log = LoggerFactory.getLogger(QifTokenMaker.class);

    @Override
    public Token getTokenList(Segment text, int startTokenType, final int startOffset) {
        if (log.isDebugEnabled()) {
            log.debug("getTokenList, text=" + text.toString());
            log.debug("  startTokenType=" + startTokenType);
            log.debug("  startOffset=" + startOffset);
        }
        return super.getTokenList(text, startTokenType, startOffset);
    }
}
