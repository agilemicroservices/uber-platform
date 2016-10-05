package org.agilemicroservices.mds.integration.spring;

import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;


public class SpringPlatformTransactionStrategy implements TransactionStrategy {
    private PlatformTransactionManager platformTransactionManager;


    public SpringPlatformTransactionStrategy() {
    }

    public SpringPlatformTransactionStrategy(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }


    @Override
    public org.agilemicroservices.mds.integration.Transaction begin() {
        TransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = platformTransactionManager.getTransaction(definition);
        Transaction transaction = new Transaction(status);
        return transaction;
    }

    @Override
    public void commit(org.agilemicroservices.mds.integration.Transaction transaction) {
        TransactionStatus status = statusOf(transaction);
        platformTransactionManager.commit(status);
    }

    @Override
    public void rollback(org.agilemicroservices.mds.integration.Transaction transaction) {
        TransactionStatus status = statusOf(transaction);
        platformTransactionManager.rollback(status);
    }

    private TransactionStatus statusOf(org.agilemicroservices.mds.integration.Transaction transaction) {
        if (transaction instanceof Transaction == false) {
            throw new IllegalArgumentException("Unrecognized transaction.");
        }
        return ((Transaction) transaction).getStatus();
    }


    public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }


    private class Transaction implements org.agilemicroservices.mds.integration.Transaction {
        TransactionStatus status;

        private Transaction(TransactionStatus status) {
            this.status = status;
        }

        private TransactionStatus getStatus() {
            return status;
        }
    }
}
