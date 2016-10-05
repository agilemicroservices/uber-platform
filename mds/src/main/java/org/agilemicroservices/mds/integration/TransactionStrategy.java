package org.agilemicroservices.mds.integration;


/**
 * <code>TransactionStrategy</code> defines the interface for objects that manage transactions on behalf of the
 * framework.
 */
public interface TransactionStrategy {

    Transaction begin();

    void commit(Transaction transaction);

    void rollback(Transaction transaction);
}
