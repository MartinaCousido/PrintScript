package org.printscript.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class JsonConfigurationMapper implements ConfigurationMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(String format) {
        return "json".equals(format);
    }

    @Override
    public Map<String, Object> read(InputStream source) throws IOException {
        return mapper.readValue(source, new TypeReference<Map<String, Object>>() {});
    }
}
