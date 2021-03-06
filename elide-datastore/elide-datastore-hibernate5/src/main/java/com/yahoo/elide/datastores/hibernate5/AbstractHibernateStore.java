/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.JPQLDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;

import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;

/**
 * Hibernate interface library.
 */
public abstract class AbstractHibernateStore implements JPQLDataStore {
    protected final SessionFactory sessionFactory;
    protected final boolean isScrollEnabled;
    protected final ScrollMode scrollMode;
    protected final HibernateTransactionSupplier transactionSupplier;

    /**
     * Constructor.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     */
    protected AbstractHibernateStore(SessionFactory aSessionFactory, boolean isScrollEnabled, ScrollMode scrollMode) {
        this(aSessionFactory, isScrollEnabled, scrollMode, HibernateTransaction::new);
    }

    /**
     * Constructor.
     *
     * Useful for extending the store and relying on existing code
     * to instantiate custom hibernate transaction.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     * @param transactionSupplier Supplier for transaction
     */
    protected AbstractHibernateStore(SessionFactory aSessionFactory,
                                     boolean isScrollEnabled,
                                     ScrollMode scrollMode,
                                     HibernateTransactionSupplier transactionSupplier) {
        this.sessionFactory = aSessionFactory;
        this.isScrollEnabled = isScrollEnabled;
        this.scrollMode = scrollMode;
        this.transactionSupplier = transactionSupplier;
    }

    /**
     * Builder object to configuration hibernate store.
     */
    public static class Builder {
        private final SessionFactory sessionFactory;
        private boolean isScrollEnabled;
        private ScrollMode scrollMode;
        private EntityManagerFactory emf;

        public Builder(final SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.emf = null;
        }

        public Builder(final EntityManagerFactory entityManagerFactory) {
            this.sessionFactory = null;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.emf = entityManagerFactory;
        }

        public Builder withScrollEnabled(final boolean isScrollEnabled) {
            this.isScrollEnabled = isScrollEnabled;
            return this;
        }

        public Builder withScrollMode(final ScrollMode scrollMode) {
            this.scrollMode = scrollMode;
            return this;
        }

        public AbstractHibernateStore build() {
            if (sessionFactory != null) {
                return new HibernateSessionFactoryStore(sessionFactory, isScrollEnabled, scrollMode);
            } else if (emf != null) {
                return new HibernateEntityManagerStore(emf, isScrollEnabled, scrollMode);
            }
            throw new IllegalStateException("Either an EntityManager or SessionFactory is required!");
        }
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (EntityType<?> type : sessionFactory.getMetamodel().getEntities()) {
            bindEntity(dictionary, type);
        }
    }

    protected void bindEntity(EntityDictionary dictionary, EntityType<?> type) {
        Type<?> mappedClass = getClassType(type.getJavaType());

        bindEntityClass(mappedClass, dictionary);
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    abstract public DataStoreTransaction beginTransaction();

    /**
     * Functional interface for describing a method to supply a custom Hibernate transaction.
     */
    @FunctionalInterface
    public interface HibernateTransactionSupplier {
        HibernateTransaction get(Session session, boolean isScrollEnabled, ScrollMode scrollMode);
    }
}
