/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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

package java.nio.file.attribute;

/**
 * File attributes associated with a file in a file system that supports
 * legacy "DOS" attributes.
 *
 * <p> The DOS attributes of a file are retrieved using a {@link
 * DosFileAttributeView} by invoking its {@link DosFileAttributeView#readAttributes
 * readAttributes} method.
 *
 * @since 1.7
 *
 * @see Attributes#readDosFileAttributes
 */

public interface DosFileAttributes
    extends BasicFileAttributes
{
    /**
     * Returns the value of the read-only attribute.
     *
     * <p> This attribute is often used as a simple access control mechanism
     * to prevent files from being deleted or updated. Whether the file system
     * or platform does any enforcement to prevent <em>read-only</em> files
     * from being updated is implementation specific.
     *
     * @return  the value of the read-only attribute
     */
    boolean isReadOnly();

    /**
     * Returns the value of the hidden attribute.
     *
     * <p> This attribute is often used to indicate if the file is visible to
     * users.
     *
     * @return  the value of the hidden attribute
     */
    boolean isHidden();

    /**
     * Returns the value of the archive attribute.
     *
     * <p> This attribute is typically used by backup programs.
     *
     * @return  the value of the archive attribute
     */
    boolean isArchive();

    /**
     * Returns the value of the system attribute.
     *
     * <p> This attribute is often used to indicate that the file is a component
     * of the operating system.
     *
     * @return  the value of the system attribute
     */
    boolean isSystem();
}
