package get.wordy.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CloseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CloseUtils.class);

    private CloseUtils() {
    }

    public static void close(AutoCloseable closeable, boolean swallowIOException) throws Exception {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                if (swallowIOException) {
                    LOG.warn("Exception thrown while closing AutoCloseable.", e);
                } else {
                    throw e;
                }
            }
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        try {
            close(closeable, true);
        } catch (Exception e) {
            LOG.warn("Exception should not have been thrown.", e);
        }
    }

}