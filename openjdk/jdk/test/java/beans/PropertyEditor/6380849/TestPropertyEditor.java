/**
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 6380849
 * @summary Tests PropertyEditor finder
 * @author Sergey Malenkov
 */

import editors.SecondBeanEditor;
import editors.ThirdBeanEditor;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

import sun.awt.SunToolkit;
import sun.beans.editors.BooleanEditor;
import sun.beans.editors.ByteEditor;
import sun.beans.editors.ColorEditor;
import sun.beans.editors.DoubleEditor;
import sun.beans.editors.EnumEditor;
import sun.beans.editors.FloatEditor;
import sun.beans.editors.FontEditor;
import sun.beans.editors.IntegerEditor;
import sun.beans.editors.LongEditor;
import sun.beans.editors.ShortEditor;
import sun.beans.editors.StringEditor;

public class TestPropertyEditor implements Runnable {

    private enum Enumeration {
        FIRST, SECOND, THIRD
    }

    private static final String[] SEARCH_PATH = { "editors" }; // NON-NLS: package name

    public static void main(String[] args) throws InterruptedException {
        TestPropertyEditor test = new TestPropertyEditor();
        test.run();
        // the following tests fails on previous build
        ThreadGroup group = new ThreadGroup("$$$"); // NON-NLS: unique thread name
        Thread thread = new Thread(group, test);
        thread.start();
        thread.join();
    }

    private static void test(Class<?> type, Class<? extends PropertyEditor> expected) {
        PropertyEditor actual = PropertyEditorManager.findEditor(type);
        if ((actual == null) && (expected != null)) {
            throw new Error("expected editor is not found");
        }
        if ((actual != null) && !actual.getClass().equals(expected)) {
            throw new Error("found unexpected editor");
        }
    }

    private boolean passed;

    public void run() {
        if (this.passed) {
            SunToolkit.createNewAppContext();
        }
        PropertyEditorManager.registerEditor(ThirdBean.class, ThirdBeanEditor.class);

        test(FirstBean.class, FirstBeanEditor.class);
        test(SecondBean.class, null);
        test(ThirdBean.class, ThirdBeanEditor.class);
        // test editors for default primitive types
        test(Byte.TYPE, ByteEditor.class);
        test(Short.TYPE, ShortEditor.class);
        test(Integer.TYPE, IntegerEditor.class);
        test(Long.TYPE, LongEditor.class);
        test(Boolean.TYPE, BooleanEditor.class);
        test(Float.TYPE, FloatEditor.class);
        test(Double.TYPE, DoubleEditor.class);
        // test editors for default object types
        test(Byte.class, ByteEditor.class);
        test(Short.class, ShortEditor.class);
        test(Integer.class, IntegerEditor.class);
        test(Long.class, LongEditor.class);
        test(Boolean.class, BooleanEditor.class);
        test(Float.class, FloatEditor.class);
        test(Double.class, DoubleEditor.class);
        test(String.class, StringEditor.class);
        test(Color.class, ColorEditor.class);
        test(Font.class, FontEditor.class);
        test(Enumeration.class, EnumEditor.class);

        PropertyEditorManager.registerEditor(ThirdBean.class, null);
        PropertyEditorManager.setEditorSearchPath(SEARCH_PATH);

        test(FirstBean.class, FirstBeanEditor.class);
        test(SecondBean.class, SecondBeanEditor.class);
        test(ThirdBean.class, ThirdBeanEditor.class);
        // test editors for default primitive types
        test(Byte.TYPE, ByteEditor.class);
        test(Short.TYPE, ShortEditor.class);
        test(Integer.TYPE, IntegerEditor.class);
        test(Long.TYPE, LongEditor.class);
        test(Boolean.TYPE, BooleanEditor.class);
        test(Float.TYPE, FloatEditor.class);
        test(Double.TYPE, DoubleEditor.class);
        // test editors for default object types
        test(Byte.class, null);
        test(Short.class, null);
        test(Integer.class, null);
        test(Long.class, null);
        test(Boolean.class, null);
        test(Float.class, null);
        test(Double.class, null);
        test(String.class, null);
        test(Color.class, null);
        test(Font.class, null);
        test(Enumeration.class, EnumEditor.class);

        this.passed = true;
    }
}
