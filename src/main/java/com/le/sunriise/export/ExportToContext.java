package com.le.sunriise.export;

import com.le.sunriise.viewer.OpenedDb;
import java.awt.Component;

public interface ExportToContext {
    Component getParentComponent();

    OpenedDb getSrcDb();
}
