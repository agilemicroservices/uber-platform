package org.agilemicroservices.mds;

import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;

import java.util.concurrent.TimeUnit;


public final class Configuration {

    private Configuration() {
        // static class
    }

    public static IdleStrategy createDefaultIdleStrategy() {
        return new SleepingIdleStrategy(TimeUnit.MILLISECONDS.toNanos(4));
        // return new BackoffIdleStrategy(20, 50, 1, TimeUnit.MICROSECONDS.toNanos(100));
    }
}
