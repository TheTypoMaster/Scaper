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
 * @bug     6650759
 * @author  mcimadamore
 * @summary Inference of formal type parameter (unused in formal parameters) is not performed
 * @compile T6650759b.java
 */

public class T6650759b {

    interface A<X extends A<X, Y>, Y extends B<X>> {}

    static class B<X extends A<X, ?>> {}

    interface C<X extends A<X, Y>, Y extends B<X>> {}

    interface D<X extends A<X, Y>, Y extends B<X>> {}

    static class E<X extends A<X, Y>, Y extends B<X>, W extends C<X, Y>> implements D<X, Y> {

        static <X extends A<X, Y>, Y extends B<X>, W extends C<X, Y>> D<X, Y> of(W w) {
            return null;
        }
    }

    <X extends A<X, Y>, Y extends B<X>, W extends C<X, Y>, Z extends D<X, Y>> Z test(W w) {
        return (Z) E.of(w);
    }
}
