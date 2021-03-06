/*
 * Copyright 1998-2007 Sun Microsystems, Inc.  All Rights Reserved.
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

/* @test
   @bug 4131223 6470354
   @summary Basic test for isHidden method
 */

import java.io.*;


public class IsHidden {

    private static String dir = System.getProperty("test.dir", ".");

    private static void ck(String path, boolean ans) throws Exception {
        File f = new File(path);
        boolean x = f.isHidden();
        if (x != ans)
            throw new Exception(path + ": expected " + ans + ", got " + x);
        System.err.println(path + " ==> " + x);
    }

    private static void testWin32() throws Exception {
        File f = new File(dir, "test");
        f.deleteOnExit();
        f.createNewFile();
        String name = f.getCanonicalPath();
        Process p = Runtime.getRuntime().exec("cmd.exe /c attrib +H " + name);
        p.waitFor();
        ck(name, true);

        ck(".foo", false);
        ck("foo", false);
    }

    private static void testUnix() throws Exception {
        ck(dir + "/IsHidden.java", false);
        ck(dir + "/.", true);
        ck(".", true);
        ck("..", true);
        ck(".foo", true);
        ck("foo", false);
        ck("", false);
    }

    public static void main(String[] args) throws Exception {
        if (File.separatorChar == '\\') testWin32();
        if (File.separatorChar == '/') testUnix();
    }

}
