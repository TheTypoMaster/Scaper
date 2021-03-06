/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

/**
 * Unit test for Files.walkFileTree to test SKIP_SIBLINGS return value.
 */

public class SkipSiblings {

    static final Random rand = new Random();
    static final Set<Path> skipped = new HashSet<Path>();

    // check if this path's directory has been skipped
    static void check(Path path) {
        if (skipped.contains(path.getParent()))
            throw new RuntimeException(path + " should not have been visited");
    }

    // indicates if the siblings of this path should be skipped
    static boolean skip(Path path) {
        Path parent = path.getParent();
        if (parent != null && rand.nextBoolean()) {
            skipped.add(parent);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]);

        Files.walkFileTree(dir, new FileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir) {
                check(dir);
                if (skip(dir))
                    return FileVisitResult.SKIP_SIBLINGS;
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult preVisitDirectoryFailed(Path dir, IOException exc) {
                throw new RuntimeException(exc);
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                check(file);
                if (skip(file))
                    return FileVisitResult.SKIP_SIBLINGS;
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult postVisitDirectory(Path dir, IOException x) {
                if (x != null)
                    throw new RuntimeException(x);
                check(dir);
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult visitFileFailed(Path file, IOException x) {
                throw new RuntimeException(x);
            }
        });
    }
}
