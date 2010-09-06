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
package org.codehaus.preon.descriptor;

import org.codehaus.preon.el.Expression;
import nl.flotsam.pecia.Documenter;
import nl.flotsam.pecia.ParaContents;
import nl.flotsam.pecia.SimpleContents;
import org.codehaus.preon.CodecDescriptor;
import org.codehaus.preon.Resolver;
import org.codehaus.preon.CodecDescriptor.Adjective;
import org.codehaus.preon.binding.Binding;
import org.codehaus.preon.buffer.ByteOrder;
import org.codehaus.preon.rendering.IdentifierRewriter;
import org.codehaus.preon.util.DocumentParaContents;
import org.codehaus.preon.util.TextUtils;

public class Documenters {

    private static final char[] HEX_SYMBOLS = "0123456789abcdef".toCharArray();

    public static Documenter<ParaContents<?>> forExpression(
            final Expression<?, Resolver> expr) {
        return new Documenter<ParaContents<?>>() {
            public void document(ParaContents<?> target) {
                if (expr == null) {
                    target.text("(unknown)");
                } else {
                    if (expr.isParameterized()) {
                        Expression<?, Resolver> simplified = expr.simplify();
                        simplified.document(new DocumentParaContents(target));
                    } else {
                        target.text(expr.eval(null).toString());
                    }
                }
            }
        };
    }

    public static Documenter<ParaContents<?>> forNumericValue(final int nrBits,
                                                              final ByteOrder byteOrder) {
        return new Documenter<ParaContents<?>>() {
            public void document(ParaContents<?> target) {
                target.text(nrBits + "bits numeric value");
                if (nrBits > 8) {
                    target.text(" (");
                    switch (byteOrder) {
                        case BigEndian:
                            target.text("big endian");
                            break;
                        case LittleEndian:
                            target.text("little endian");
                            break;
                    }
                    target.text(")");
                }
            }
        };
    }

    public static Documenter<ParaContents<?>> forByteOrder(
            final ByteOrder byteOrder) {
        return new Documenter<ParaContents<?>>() {
            public void document(ParaContents<?> target) {
                target.text(byteOrder.asText());
            }
        };
    }

    public static Documenter<ParaContents<?>> forBits(
            final Expression<Integer, Resolver> expr) {
        if (expr == null) {
            return new Documenter<ParaContents<?>>() {
                public void document(ParaContents<?> target) {
                    target.text("(unknown)");
                }
            };
        } else {
            if (expr.isParameterized()) {
                return forExpression(expr);
            } else {
                return new Documenter<ParaContents<?>>() {
                    public void document(ParaContents<?> target) {
                        int nrBits = expr.eval(null);
                        target.text(TextUtils.bitsToText(nrBits));
                    }
                };
            }
        }
    }

    public static Documenter<ParaContents<?>> forBindingName(
            final Binding binding, final IdentifierRewriter rewriter) {
        return new Documenter<ParaContents<?>>() {
            public void document(ParaContents<?> target) {
                target.term(binding.getId(), rewriter
                        .rewrite(binding.getName()));
            }
        };
    }

    public static Documenter<SimpleContents<?>> forBindingDescription(
            final Binding binding) {
        return new Documenter<SimpleContents<?>>() {
            public void document(SimpleContents<?> target) {
                binding.describe(target);
            }
        };
    }

    public static Documenter<ParaContents<?>> forHexSequence(final byte[] sequence) {
        return new Documenter<ParaContents<?>>() {
            public void document(ParaContents<?> target) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < sequence.length; i++) {
                    if (i != 0) {
                        builder.append(' ');
                    }
                    builder.append(HEX_SYMBOLS[((sequence[i] >> 4) & 0xf)]);
                    builder.append(HEX_SYMBOLS[(sequence[i] & 0x0f)]);
                }
                target.text(builder.toString());
            }
        };
    }

    public static Documenter<ParaContents<?>> forDescriptor(final CodecDescriptor descriptor) {
        return new Documenter<ParaContents<?>>() {

            public void document(ParaContents<?> target) {
                descriptor.reference(Adjective.THE, false);
            }

        };
    }

}