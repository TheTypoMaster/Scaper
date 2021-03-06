/*
 * Copyright 2003 Sun Microsystems, Inc.  All Rights Reserved.
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
 *
 */

package sun.jvm.hotspot.asm.x86;

import sun.jvm.hotspot.asm.*;

public class X86MoveStoreInstruction extends X86MemoryInstruction
                                          implements StoreInstruction {
   final protected Register[] storeSources;

   public X86MoveStoreInstruction(String name, Address address, X86Register register, int dataType, int size, int prefixes) {
      super(name, address, register, dataType, size, prefixes);
      storeSources = new Register[1];
      storeSources[0] = register;
   }

   protected String initDescription() {
      StringBuffer buf = new StringBuffer();
      buf.append(getPrefixString());
      buf.append(getName());
      buf.append(spaces);
      buf.append(address.toString());
      buf.append(comma);
      buf.append(register.toString());
      return buf.toString();
   }

   public int getDataType() {
      return dataType;
   }

   public Address getStoreDestination() {
      return address;
   }

   public Register[] getStoreSources() {
      return storeSources;
   }

   public boolean isStore() {
      return true;
   }
}
