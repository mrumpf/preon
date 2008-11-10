/*
 * Copyright (C) 2008 Wilfred Springer
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
 * Preon; see the file COPYING. If not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

package nl.flotsam.preon.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.flotsam.limbo.BindingException;
import nl.flotsam.limbo.Document;
import nl.flotsam.limbo.Expression;
import nl.flotsam.limbo.Expressions;
import nl.flotsam.limbo.Reference;
import nl.flotsam.limbo.ctx.MultiReference;
import nl.flotsam.limbo.util.StringBuilderDocument;
import nl.flotsam.preon.Resolver;
import nl.flotsam.preon.ResolverContext;
import nl.flotsam.preon.binding.Binding;
import nl.flotsam.preon.limbo.ArrayElementReference;
import nl.flotsam.preon.limbo.ObjectResolverContext;
import nl.flotsam.preon.limbo.OuterReference;
import nl.flotsam.preon.limbo.PropertyReference;
import nl.flotsam.preon.util.ParaContentsDocument;

/**
 * A {@link ResolverContext} based on a collection of {@link Binding Bindings}.
 * 
 * @author Wilfred Springer (wis)
 * 
 */
public class BindingsContext implements ObjectResolverContext {

    /**
     * All {@link Binding}s.
     */
    private List<Binding> orderedBindings;

    /**
     * All {@link Binding}s, indexed by their name.
     */
    private HashMap<String, Binding> bindingsByName;

    /**
     * The "outer" {@link ResolverContext}.
     */
    private ResolverContext outer;

    /**
     * Constructs a new instance.
     * 
     * @param type
     * @param outer
     *            The "outer" {@link ResolverContext}.
     */
    public BindingsContext(Class<?> type, ResolverContext outer) {
        this.orderedBindings = new ArrayList<Binding>();
        this.bindingsByName = new HashMap<String, Binding>();
        this.outer = outer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.flotsam.preon.limbo.ObjectResolverContext#add(java.lang.String,
     * nl.flotsam.preon.binding.Binding)
     */
    public void add(String name, Binding binding) {
        orderedBindings.add(binding);
        bindingsByName.put(name, binding);
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.flotsam.limbo.ReferenceContext#selectAttribute(java.lang.String)
     */
    public Reference<Resolver> selectAttribute(String name)
            throws BindingException {
        if ("outer".equals(name)) {
            return new OuterReference(outer, this);
        } else {
            Binding binding = bindingsByName.get(name);

            if (binding == null) {
                throw new BindingException(
                        "Failed to create binding for bound data called "
                                + name);
            }
            return new BindingReference(binding);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.flotsam.limbo.ReferenceContext#selectItem(java.lang.String)
     */
    public Reference<Resolver> selectItem(String index) throws BindingException {
        throw new BindingException("Cannot resolve index on BindingContext.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * nl.flotsam.limbo.ReferenceContext#selectItem(nl.flotsam.limbo.Expression)
     */
    public Reference<Resolver> selectItem(Expression<Integer, Resolver> index)
            throws BindingException {
        StringBuilder builder = new StringBuilder();
        index.document(new StringBuilderDocument(builder));
        throw new BindingException("Cannot resolve index on BindingContext.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.flotsam.limbo.Descriptive#document(nl.flotsam.limbo.Document)
     */
    public void document(Document target) {
        if (bindingsByName.size() > 0) {
            target.text("one of ");
            boolean passedFirst = false;
            for (Binding binding : bindingsByName.values()) {
                if (passedFirst) {
                    target.text(", ");
                }
                target.text(binding.getName());
                passedFirst = true;
            }
        } else {
            target.text("no variables");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * nl.flotsam.preon.limbo.ObjectResolverContext#getResolver(java.lang.Object
     * , nl.flotsam.preon.Resolver)
     */
    public Resolver getResolver(Object context, Resolver resolver) {
        return new BindingsResolver(context, resolver);
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.flotsam.preon.limbo.ObjectResolverContext#getBindings()
     */
    public List<Binding> getBindings() {
        return orderedBindings;
    }

    /**
     * A {@link Reference} referring to a {@link Binding}.
     */
    private class BindingReference implements Reference<Resolver> {

        /**
         * The {@link Binding} it refers to.
         */
        private Binding binding;

        /**
         * The most specific supertype of anything that can be returned from
         * this {@link Reference}.
         */
        private Class<?> commonType;

        /**
         * Constructs a new instance.
         * 
         * @param binding
         *            The {@link Binding}.
         */
        public BindingReference(Binding binding) {
            this.binding = binding;
            commonType = binding.getType();
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.limbo.Reference#getReferenceContext()
         */
        public ResolverContext getReferenceContext() {
            return BindingsContext.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.limbo.Reference#isAssignableTo(java.lang.Class)
         */
        public boolean isAssignableTo(Class<?> type) {

            for (Class<?> bound : binding.getTypes()) {

                if (bound.isAssignableFrom(type)) {
                    return true;
                }
            }

            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.limbo.Reference#resolve(java.lang.Object)
         */
        public Object resolve(Resolver context) {
            try {
                String name = binding.getName();
                return context.get(name);
            } catch (IllegalArgumentException e) {
                throw new BindingException("Failed to bind to "
                        + binding.getName(), e);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * nl.flotsam.limbo.ReferenceContext#selectAttribute(java.lang.String)
         */
        @SuppressWarnings("unchecked")
        public Reference<Resolver> selectAttribute(String name) {
            Reference<Resolver>[] references = new Reference[binding.getTypes().length];
            int i = 0;

            for (Class<?> bound : binding.getTypes()) {
                references[i] = new PropertyReference(this, bound, name,
                        BindingsContext.this);
                i++;
            }

            return new MultiReference<Resolver>(references);
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.limbo.ReferenceContext#selectItem(java.lang.String)
         */
        public Reference<Resolver> selectItem(String index) {
            Expression<Integer, Resolver> expr;
            expr = Expressions.createInteger(BindingsContext.this, index);

            return selectItem(expr);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * nl.flotsam.limbo.ReferenceContext#selectItem(nl.flotsam.limbo.Expression
         * )
         */
        @SuppressWarnings("unchecked")
        public Reference<Resolver> selectItem(
                Expression<Integer, Resolver> index) {

            if (binding.getTypes().length > 1) {
                Reference<Resolver>[] references = new Reference[binding
                        .getTypes().length];

                for (int i = 0; i < binding.getTypes().length; i++) {
                    references[i] = new ArrayElementReference(this, binding
                            .getTypes()[i], index, BindingsContext.this);
                }

                return new MultiReference<Resolver>(references);
            } else {
                return new ArrayElementReference(this, binding.getType()
                        .getComponentType(), index, BindingsContext.this);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.limbo.Descriptive#document(nl.flotsam.limbo.Document)
         */
        public void document(final Document target) {
            binding.writeReference(new ParaContentsDocument(target));
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.limbo.Reference#getType()
         */
        public Class<?> getType() {
            return commonType;
        }

    }

    /**
     * A {@link Resolver} resolving to bindings. In addition, it also resolves
     * outer.
     * 
     * @author Wilfred Springer (wis)
     * 
     */
    private class BindingsResolver implements Resolver {

        /**
         * The instance on which the objects need to be resolved.
         */
        private Object context;

        /**
         * The outer Resolver.
         */
        private Resolver outer;

        /**
         * Constructs a new instance.
         * 
         * @param context
         *            The object for resolving bindings.
         * @param outer
         *            A reference to the outer context.
         */
        public BindingsResolver(Object context, Resolver outer) {
            this.context = context;
            this.outer = outer;
        }

        /*
         * (non-Javadoc)
         * 
         * @see nl.flotsam.preon.Resolver#get(java.lang.String)
         */
        public Object get(String name) {
            if ("outer".equals(name)) {
                return outer;
            } else {

                if (bindingsByName.containsKey(name)) {
                    Binding binding = bindingsByName.get(name);

                    if (context == null) {
                        StringBuilderDocument document = new StringBuilderDocument();
                        binding.describe(new ParaContentsDocument(document));
                        throw new BindingException("Failed to resolve "
                                + document.toString()
                                + " due to incomplete context.");
                    }

                    try {
                        return binding.get(context);
                    } catch (IllegalArgumentException e) {
                        throw new BindingException("Failed to bind to "
                                + binding.getName(), e);
                    } catch (IllegalAccessException e) {
                        throw new BindingException("Forbidded to access "
                                + binding.getName(), e);
                    }
                } else {
                    throw new BindingException("Failed to resolve " + name
                            + " on " + context.getClass());
                }
            }
        }

        /**
         * Returns the "outer" {@link Resolver}.
         */
        public Resolver getOuter() {
            return outer;
        }

        /**
         * Returns the {@link Resolver} from which all new references need to be
         * constructed.
         */
        public Resolver getOriginalResolver() {
            return this;
        }

    }

}
