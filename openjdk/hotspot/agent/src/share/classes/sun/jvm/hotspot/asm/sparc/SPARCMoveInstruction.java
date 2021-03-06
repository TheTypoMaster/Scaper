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

public class SPARCMoveInstruction extends SPARCFormat3AInstruction
    implements MoveInstruction, RTLOperations {

    public SPARCMoveInstruction(String name, int opcode, ImmediateOrRegister operand2, SPARCRegister rd) {
        super(name, opcode, null, operand2, rd);
    }

    protected String getDescription() {
        StringBuffer buf = new StringBuffer();
        if (operand2 == SPARCRegisters.G0) {
            buf.append("clr");
            buf.append(spaces);
            buf.append(rd.toString());
        } else {
            buf.append("mov");
            buf.append(spaces);
            buf.append(getOperand2String());
            buf.append(comma);
            buf.append(rd.toString());
        }

        return buf.toString();
    }

    public Register getMoveDestination() {
        return getDestinationRegister();
    }

    public ImmediateOrRegister getMoveSource() {
        return operand2;
    }

    public boolean isConditional() {
        return false;
    }

    public boolean isMove() {
        return true;
    }
}
