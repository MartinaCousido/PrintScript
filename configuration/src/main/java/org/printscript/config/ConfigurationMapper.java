package org.printscript.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface ConfigurationMapper {

    boolean supports(String format);

    /** Reads one configuration object. The caller owns and closes the source. */
    Map<String, Object> read(InputStream source) throws IOException;
}
