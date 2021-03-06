/*
 * Copyright 2002 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.jvm.hotspot.asm.sparc;

import sun.jvm.hotspot.asm.*;

class JmplDecoder extends MemoryInstructionDecoder {
    JmplDecoder() {
        super(JMPL, "jmpl", RTLDT_UNSIGNED_WORD);
    }

    Instruction decodeMemoryInstruction(int instruction, SPARCRegisterIndirectAddress addr,
                       SPARCRegister rd,  SPARCInstructionFactory factory) {
        // this may be most probably indirect call or ret or retl
        Instruction instr = null;
        if (rd == SPARCRegisters.O7) {
            instr = factory.newIndirectCallInstruction(addr, rd);
        } else if (rd == SPARCRegisters.G0) {
            int disp = (int) addr.getDisplacement();
            Register base = addr.getBase();
            if (base == SPARCRegisters.I7 && disp == 8) {
                instr = factory.newReturnInstruction(addr, rd, false /* not leaf */);
            } else if (base == SPARCRegisters.O7 && disp == 8) {
                instr = factory.newReturnInstruction(addr, rd, true /* leaf */);
            } else {
                instr = factory.newJmplInstruction(addr, rd);
            }
        } else {
            instr = factory.newJmplInstruction(addr, rd);
        }
        return instr;
    }
}
