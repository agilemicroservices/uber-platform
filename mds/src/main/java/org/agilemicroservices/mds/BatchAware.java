package org.agilemicroservices.mds;


/**
 * <code>BatchAware</code> may optionally be implemented by services to receive callbacks when a batch of messages is
 * committed.
 */
public interface BatchAware {

    void endOfBatch();
}
