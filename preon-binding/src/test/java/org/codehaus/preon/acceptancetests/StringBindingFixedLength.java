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
import static org.junit.Assert.fail;

public class StringBindingFixedLength {
    @Test
    public void defaultEncodingIsUsAscii() throws NoSuchFieldException {
        final BoundString strField = ClassWithFixedLengthStringOfThreeBytes.class.getField("str").getAnnotation(BoundString.class);

        assertThat(strField.encoding(), is("US-ASCII"));
    }

    @Test
    public void canDecodeAnyLengthStrings() throws DecodingException {
        assertCanDecodeShortFixedLengthString();
        assertCanDecodeLongFixedLengthString();
    }

    @Test
    public void canDecodeStringsFromJvmSupportedCharsets() throws DecodingException {
        assertCanDecodeUTF8FixedLengthString();
        assertCanDecodeUTF16BEFixedLengthString();
    }

    @Test
    public void canDecodeStringsUsingMatchParameter() throws DecodingException {
        assertCanDecodeStringWithSuccessfulMatch();
        assertThrowsDecodingExceptionOnInvalidMatch();
    }

    private static void assertCanDecodeShortFixedLengthString() throws DecodingException {
        final byte[] shortInput = {'a','b','c'};
        final ClassWithFixedLengthStringOfThreeBytes shortResult = decodeObjectFromInput(ClassWithFixedLengthStringOfThreeBytes.class, shortInput);

        assertThat(shortResult.str, is("abc"));
    }

    private static void assertCanDecodeLongFixedLengthString() throws DecodingException {
        final int LONG_INPUT_SIZE_BYTES = 1024*1024*10; // 10mb string
        final byte[] longInput = generateArrayContaining((byte) 'a', LONG_INPUT_SIZE_BYTES); // "aaaa..." LONG_INPUT_SIZE_BYTES times

        final ClassWithFixedLengthStringOf10mb longResult = decodeObjectFromInput(ClassWithFixedLengthStringOf10mb.class, longInput);

        final String matchStr = String.format("\\Aa{%d}\\z", LONG_INPUT_SIZE_BYTES);
        assertTrue("Couldn't match string", Pattern.matches(matchStr, longResult.str));
    }

    private static void assertCanDecodeUTF8FixedLengthString() throws DecodingException {
        final byte[] input = {
                (byte)0x54,                         // 1 UTF-8 code unit: T
                (byte)0xC3, (byte)0x9F,             // 2 UTF-8 code units: ??
                (byte)0xE6, (byte)0x9D, (byte)0xB1  // 3 UTF-8 code units: ???
        };
        final ClassWithFixedLengthUTF8StringOfSixBytes result = decodeObjectFromInput(ClassWithFixedLengthUTF8StringOfSixBytes.class, input);

        assertThat(result.str, is("T?????"));
    }

    private static void assertCanDecodeUTF16BEFixedLengthString() throws DecodingException {
        final byte[] input = {
                (byte)0x00, (byte)0x54, // 1 UTF-8 code unit: T
                (byte)0x00, (byte)0xDF, // 1 UTF-16 code unit: ??
                (byte)0x67, (byte)0x71  // 1 UTF-16 code unit: ???
        };
        final ClassWithFixedLengthUTF16BEStringOfSixBytes result = decodeObjectFromInput(ClassWithFixedLengthUTF16BEStringOfSixBytes.class, input);

        assertThat(result.str, is("T?????"));
    }

    private static void assertCanDecodeStringWithSuccessfulMatch() throws DecodingException {
        final byte[] input = {'A','b','C'};
        ClassWithStringOfThreeBytesAndMatch2 result = decodeObjectFromInput(ClassWithStringOfThreeBytesAndMatch2.class, input);

        assertThat(result.str, is("AbC"));
    }

    private static void assertThrowsDecodingExceptionOnInvalidMatch() throws DecodingException {
        final byte[] input = {'A','b','C'};
        
        try {
            decodeObjectFromInput(ClassWithStringOfThreeBytesAndMatch1.class, input);
            fail("Expected DecodingException");
        }
        catch (DecodingException ignored) {}
    }

    public static class ClassWithStringOfThreeBytesAndMatch2 {
        @BoundString(size = "3", match = "AbC")
        public String str;
    }
    
    public static class ClassWithStringOfThreeBytesAndMatch1 {
        @BoundString(size = "3", match = "AbCd")
        public String str;
    }

    public static class ClassWithFixedLengthUTF16BEStringOfSixBytes {
        @BoundString(encoding = "UTF-16BE", size = "6")
        public String str;
    }

    public static class ClassWithFixedLengthUTF8StringOfSixBytes {
        @BoundString(encoding = "UTF-8", size = "6")
        public String str;
    }
    
    public static class ClassWithFixedLengthStringOfThreeBytes {
        @BoundString(size = "3")
        public String str;
    }

    public static class ClassWithFixedLengthStringOf10mb {
        @BoundString(size = "1024*1024*10")
        public String str;
    }
}
