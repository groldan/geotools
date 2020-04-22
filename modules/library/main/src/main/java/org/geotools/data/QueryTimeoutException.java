package org.geotools.data;

import java.io.IOException;

public class QueryTimeoutException extends IOException {
    private static final long serialVersionUID = 7118969983651291349L;

    public QueryTimeoutException(String message) {
        super(message);
    }

    public QueryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryTimeoutException(Throwable cause) {
        super(cause);
    }
}
