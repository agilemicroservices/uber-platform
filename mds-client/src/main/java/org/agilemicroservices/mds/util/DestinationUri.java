package org.agilemicroservices.mds.util;


public final class DestinationUri {
    public static final String QUEUE_SCHEME = "queue";
    public static final String TOPIC_SCHEME = "topic";

    private String scheme;
    private String name;
    private String uri;


    public DestinationUri(String uri) {
        this.uri = uri;
        if (uri.startsWith("queue:")) {
            scheme = QUEUE_SCHEME;
            name = uri.substring(QUEUE_SCHEME.length() + 1);
        } else if (uri.startsWith("topic:")) {
            scheme = TOPIC_SCHEME;
            name = uri.substring(TOPIC_SCHEME.length() + 1);
        } else {
            throw new IllegalArgumentException("Invalid destination URI '" + uri + "'.");
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (null == o) {
            return false;
        }

        DestinationUri that = (DestinationUri) o;

        return uri.equals(that.uri);

    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    public String getScheme() {
        return scheme;
    }

    public boolean isQueue() {
        return QUEUE_SCHEME.equals(scheme);
    }

    public boolean isTopic() {
        return TOPIC_SCHEME.equals(scheme);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return uri;
    }
}
