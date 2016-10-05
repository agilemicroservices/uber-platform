package org.agilemicroservices.mds.integration.quickfixj;

import org.agilemicroservices.mds.integration.SerializationStrategy;
import quickfix.DataDictionary;
import quickfix.DefaultDataDictionaryProvider;
import quickfix.InvalidMessage;
import quickfix.field.ApplVerID;


// TODO support serialization of more message types
public class QuickfixjSerializationStrategy implements SerializationStrategy {

    public String serialize(Object value) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialize(String str, Class<T> clazz) {
        DefaultDataDictionaryProvider provider = new DefaultDataDictionaryProvider(true);
        ApplVerID fixVersion = parseApplVerID(str);
        DataDictionary dictionary = provider.getApplicationDataDictionary(fixVersion);

        quickfix.Message message = new quickfix.Message();
        try {
            message.fromString(str, dictionary, false);
        } catch (InvalidMessage e) {
            throw new IllegalStateException(e);
        }

        return (T) message;
    }

    private static ApplVerID parseApplVerID(String str) {
        ApplVerID fixVersion;
        if (str.startsWith("8=FIX.4.0\u0001")) {
            fixVersion = new ApplVerID(ApplVerID.FIX40);
        } else if (str.startsWith("8=FIX.4.1\u0001")) {
            fixVersion = new ApplVerID(ApplVerID.FIX41);
        } else if (str.startsWith("8=FIX.4.2\u0001")) {
            fixVersion = new ApplVerID(ApplVerID.FIX42);
        } else if (str.startsWith("8=FIX.4.3\u0001")) {
            fixVersion = new ApplVerID(ApplVerID.FIX43);
        } else if (str.startsWith("8=FIX.4.4\u0001")) {
            fixVersion = new ApplVerID(ApplVerID.FIX44);
        } else {
            throw new IllegalStateException("Unsupported FIX version " + str);
        }
        return fixVersion;
    }
}
