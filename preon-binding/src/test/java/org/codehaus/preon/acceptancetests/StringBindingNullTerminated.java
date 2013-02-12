/**
 * Copyright (C) 2009-2010 Wilfred Springer
 *
 * This file is part of Preon.
 *
 * Preon is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * Preon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Preon; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.codehaus.preon.acceptancetests;

import org.codehaus.preon.DecodingException;
import org.codehaus.preon.annotation.BoundString;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.codehaus.preon.acceptancetests.TestUtils.decodeObjectFromInput;
import static org.codehaus.preon.acceptancetests.TestUtils.generateArrayContaining;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StringBindingNullTerminated {
    @Test
    public void defaultEncodingIsUsAscii() throws NoSuchFieldException {
        final BoundString strField = ClassWithNullTerminatedString.class.getField("str").getAnnotation(BoundString.class);

        assertThat(strField.encoding(), is("US-ASCII"));
    }

    @Test
    public void canDecodeAnyLengthStrings() throws DecodingException {
        assertCanDecodeShortNullTerminatedString();
        assertCanDecodeLongNullTerminatedString();
    }

    @Test
    public void canDecodeStringsFromJvmSupportedCharsets() throws DecodingException {
        assertCanDecodeUTF8NullTerminatedString();
        assertCanDecodeUTF16BENullTerminatedString();
    }

    private static void assertCanDecodeShortNullTerminatedString() throws DecodingException {
        final byte[] shortInput = {'a','b','c','\0'};
        final ClassWithNullTerminatedString shortResult = decodeObjectFromInput(ClassWithNullTerminatedString.class, shortInput);

        assertThat(shortResult.str, is("abc"));
    }

    private static void assertCanDecodeLongNullTerminatedString() throws DecodingException {
        final int LONG_INPUT_SIZE_BYTES = 1024*1024*10; // 10mb string
        final byte[] longInput = generateArrayContaining((byte)'a', LONG_INPUT_SIZE_BYTES); // "aaaa..." LONG_INPUT_SIZE_BYTES times
        longInput[LONG_INPUT_SIZE_BYTES-1] = '\0'; // Null termination character

        final ClassWithNullTerminatedString longResult = decodeObjectFromInput(ClassWithNullTerminatedString.class, longInput);

        final String matchStr = String.format("\\Aa{%d}\\z", LONG_INPUT_SIZE_BYTES - 1);
        assertTrue("Couldn't match string", Pattern.matches(matchStr, longResult.str));
    }

    private static void assertCanDecodeUTF8NullTerminatedString() throws DecodingException {
        final byte[] input = {
                (byte)0x54,                         // 1 UTF-8 code unit: T
                (byte)0xC3, (byte)0x9F,             // 2 UTF-8 code units: ??
                (byte)0xE6, (byte)0x9D, (byte)0xB1, // 3 UTF-8 code units: ???
                (byte)0x00                          // Null termination
        };
        final ClassWithNullTerminatedUTF8String result = decodeObjectFromInput(ClassWithNullTerminatedUTF8String.class, input);

        assertThat(result.str, is("T?????"));
    }

    private static void assertCanDecodeUTF16BENullTerminatedString() throws DecodingException {
        final byte[] input = {
                (byte)0x00, (byte)0x54, // 1 UTF-16 code unit: T
                (byte)0x00, (byte)0xDF, // 1 UTF-16 code unit: ??
                (byte)0x67, (byte)0x71, // 1 UTF-16 code unit: ???
                (byte)0x00, (byte)0x00  // Null termination
        };
        final ClassWithNullTerminatedUTF16BEString result = decodeObjectFromInput(ClassWithNullTerminatedUTF16BEString.class, input);

        assertThat(result.str, is("T?????"));
    }

    public static class ClassWithNullTerminatedUTF16BEString {
        @BoundString(encoding = "UTF-16BE")
        public String str;
    }

    public static class ClassWithNullTerminatedUTF8String {
        @BoundString(encoding = "UTF-8")
        public String str;
    }

    public static class ClassWithNullTerminatedString {
        @BoundString
        public String str;
    }
}
