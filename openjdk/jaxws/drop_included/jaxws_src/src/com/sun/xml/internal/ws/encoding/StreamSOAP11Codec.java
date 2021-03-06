/*
 * Copyright 2005-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.xml.internal.ws.encoding;

import com.sun.xml.internal.stream.buffer.XMLStreamBuffer;
import com.sun.xml.internal.ws.api.SOAPVersion;
import com.sun.xml.internal.ws.api.pipe.ContentType;
import com.sun.xml.internal.ws.message.stream.StreamHeader;
import com.sun.xml.internal.ws.message.stream.StreamHeader11;

import javax.xml.stream.XMLStreamReader;
import java.util.Collections;
import java.util.List;

/**
 * {@link StreamSOAPCodec} for SOAP 1.1.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class StreamSOAP11Codec extends StreamSOAPCodec {
    public static final String SOAP11_MIME_TYPE = "text/xml";
    public static final String SOAP11_CONTENT_TYPE = SOAP11_MIME_TYPE+";charset=\"utf-8\"";

    private static final List<String> expectedContentTypes = Collections.singletonList(SOAP11_MIME_TYPE);

    /*package*/  StreamSOAP11Codec() {
        super(SOAPVersion.SOAP_11);
    }

    public String getMimeType() {
        return SOAP11_MIME_TYPE;
    }

    @Override
    protected final StreamHeader createHeader(XMLStreamReader reader, XMLStreamBuffer mark) {
        return new StreamHeader11(reader, mark);
    }

    public static final ContentTypeImpl defaultContentType =
            new ContentTypeImpl(SOAP11_CONTENT_TYPE, "");

    @Override
    protected ContentType getContentType(String soapAction) {
        return new ContentTypeImpl(SOAP11_CONTENT_TYPE, soapAction);
    }

    protected List<String> getExpectedContentTypes() {
        return expectedContentTypes;
    }
}
