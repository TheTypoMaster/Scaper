/*
 * Copyright 2005-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package javax.lang.model.type;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.io.ObjectInputStream;
import java.io.IOException;
import javax.lang.model.element.Element;


/**
 * Thrown when an application attempts to access a sequence of {@link
 * Class} objects each corresponding to a {@link TypeMirror}.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see MirroredTypeException
 * @see Element#getAnnotation(Class)
 * @since 1.6
 */
public class MirroredTypesException extends RuntimeException {

    private static final long serialVersionUID = 269;

    private transient List<? extends TypeMirror> types; // cannot be serialized

    /**
     * Constructs a new MirroredTypesException for the specified types.
     *
     * @param types  the types being accessed
     */
    public MirroredTypesException(List<? extends TypeMirror> types) {
        super("Attempt to access Class objects for TypeMirrors " +
              (types = // defensive copy
               new ArrayList<TypeMirror>(types)).toString() );
        this.types = Collections.unmodifiableList(types);
    }

    /**
     * Returns the type mirrors corresponding to the types being accessed.
     * The type mirrors may be unavailable if this exception has been
     * serialized and then read back in.
     *
     * @return the type mirrors in construction order, or {@code null} if unavailable
     */
    public List<? extends TypeMirror> getTypeMirrors() {
        return types;
    }

    /**
     * Explicitly set all transient fields.
     */
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        types = null;
    }
}
