package get.wordy.core.db;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class CloseUtils {

    private static final Logger LOG = Logger.getLogger(CloseUtils.class.getName());

    private CloseUtils() {
    }

    public static void close(AutoCloseable closeable, boolean swallowIOException) throws Exception {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                if (swallowIOException) {
                    LOG.log(Level.WARNING, "Exception thrown while closing AutoCloseable.", e);
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
            LOG.log(Level.WARNING, "Exception should not have been thrown.", e);
        }
    }

}