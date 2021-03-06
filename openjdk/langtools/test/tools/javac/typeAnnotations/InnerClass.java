/*
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
 * @bug 6843077
 * @summary compiler crashes when visiting inner classes
 * @author Mahmood Ali
 * @compile -source 1.7 InnerClass.java
 */

class InnerClass {

    InnerClass() {}
    InnerClass(Object o) {}

    private void a() {
        new Object() {
            public <R> void method() { }
        };
    }

    Object f1 = new InnerClass() {
            <R> void method() { }
        };

    Object f2 = new InnerClass() {
            <@A R> void method() { }
        };

    Object f3 = new InnerClass(null) {
            <R> void method() { }
        };

    Object f4 = new InnerClass(null) {
            <@A R> void method() { }
        };
    @interface A { }
}
