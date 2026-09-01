package org.printscript.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class YamlConfigurationMapper implements ConfigurationMapper {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Override
    public boolean supports(String format) {
        return "yaml".equals(format) || "yml".equals(format);
    }

    @Override
    public Map<String, Object> read(InputStream source) throws IOException {
        return mapper.readValue(source, new TypeReference<Map<String, Object>>() {});
    }
}
