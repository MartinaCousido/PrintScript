package org.printscript.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface ConfigurationMapper {

    boolean supports(String format);

    Map<String, Object> read(InputStream source) throws IOException;
}
