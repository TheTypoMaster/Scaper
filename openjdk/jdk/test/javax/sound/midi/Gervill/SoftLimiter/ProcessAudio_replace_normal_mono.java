/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
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

/* @test
   @summary Test SoftLimiter processAudio method */

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class ProcessAudio_replace_normal_mono {

    private static void assertEquals(Object a, Object b) throws Exception
    {
        if(!a.equals(b))
            throw new RuntimeException("assertEquals fails!");
    }

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        SoftSynthesizer synth = new SoftSynthesizer();
        synth.openStream(new AudioFormat(44100, 16, 1, true, false), null);

        SoftAudioBuffer in1 = new SoftAudioBuffer(250, synth.getFormat());
        SoftAudioBuffer out1 = new SoftAudioBuffer(250, synth.getFormat());

        float[] testdata1 = new float[in1.getSize()];
        float[] n1a = in1.array();
        float[] out1a = out1.array();
        for (int i = 0; i < n1a.length; i++) {
            testdata1[i] = (float)Math.sin(i*0.3)*0.9f;
            n1a[i] = testdata1[i];
            out1a[i] = 1;
        }

        SoftLimiter limiter = new SoftLimiter();
        limiter.init(44100, 147);
        limiter.setMixMode(false);
        limiter.setInput(0, in1);
        limiter.setOutput(0, out1);
        limiter.processControlLogic();
        limiter.processAudio();
        limiter.processControlLogic();
        limiter.processAudio();
        // Limiter should delay audio by one buffer,
        // and there should almost no different in output v.s. input
        for (int i = 0; i < n1a.length; i++) {
            if(Math.abs(out1a[i] - testdata1[i]) > 0.00001)
                throw new Exception("input != output");
        }

        synth.close();
    }
}
