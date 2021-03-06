/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 * @summary new type annotation location: constructor type args
 * @author Mahmood Ali
 * @compile -source 1.7 ConstructorTypeArgs.java
 */

class ConstructorTypeArgs {
  void oneArg() {
    new @A MyList<@A String>();
    new MyList<@A MyList<@B(0) String>>();
  }

  void twoArg() {
    new MyMap<String, String>();
    new MyMap<@A String, @B(0) MyList<@A String>>();
  }

  void withArraysIn() {
    new MyList<String[]>();
    new MyList<@A String @B(0) [] @A []>();

    new MyMap<@A String[], @B(0) MyList<@A String> @A []>();
  }
}

class MyList<E> { }
class MyMap<K, V> { }

@interface A { }
@interface B { int value(); }
