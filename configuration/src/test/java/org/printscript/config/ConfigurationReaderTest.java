package org.printscript.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfigurationReaderTest {

    private static final String JSON = "json";
    private static final String YAML = "yaml";
    private static final String FORMAT = "identifier_format";
    private static final String CAMEL = "camel case";
    private static final String OTRA = "otra";

    private final ConfigurationReader reader =
            new ConfigurationReader(
                    List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));

    private static InputStream streamOf(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsAJsonConfiguration() {
        RulesConfiguration configuration =
                reader.read(
                        streamOf("{\"" + FORMAT + "\": \"" + CAMEL + "\", \"" + OTRA + "\": true}"),
                        JSON);

        assertEquals(Optional.of(CAMEL), configuration.getString(FORMAT));
        assertEquals(Optional.of(true), configuration.getBoolean(OTRA));
    }

    @Test
    void readsAYamlConfiguration() {
        RulesConfiguration configuration =
                reader.read(streamOf(FORMAT + ": " + CAMEL + "\n" + OTRA + ": true\n"), YAML);

        assertEquals(Optional.of(CAMEL), configuration.getString(FORMAT));
        assertEquals(Optional.of(true), configuration.getBoolean(OTRA));
    }

    @Test
    void standardReaderSupportsBothBuiltInFormats() {
        ConfigurationReader standard = ConfigurationReader.standard();

        assertEquals(
                Optional.of(CAMEL),
                standard.read(streamOf("{\"" + FORMAT + "\": \"" + CAMEL + "\"}"), JSON)
                        .getString(FORMAT));
        assertEquals(
                Optional.of(CAMEL),
                standard.read(streamOf(FORMAT + ": " + CAMEL), YAML).getString(FORMAT));
    }

    @Test
    void readerRequiresAtLeastOneMapper() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationReader(List.of()));
    }

    @Test
    void readerRejectsMissingSourceOrFormat() {
        assertThrows(NullPointerException.class, () -> reader.read(null, JSON));
        assertThrows(NullPointerException.class, () -> reader.read(streamOf("{}"), null));
    }

    @Test
    void theShortYamlExtensionIsAlsoYaml() {
        RulesConfiguration configuration = reader.read(streamOf(OTRA + ": false\n"), "yml");

        assertEquals(Optional.of(false), configuration.getBoolean(OTRA));
    }

    @Test
    void theFormatIsMatchedRegardlessOfCase() {
        RulesConfiguration configuration =
                reader.read(streamOf("{\"" + OTRA + "\": true}"), "JSON");

        assertEquals(Optional.of(true), configuration.getBoolean(OTRA));
    }

    @Test
    void anEmptyConfigurationIsValidAndHasNoKeys() {
        RulesConfiguration configuration = reader.read(streamOf("{}"), JSON);

        assertTrue(configuration.getString(FORMAT).isEmpty());
    }

    @Test
    void anUnsupportedFormatFailsNamingTheFormat() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reader.read(streamOf("clave = 1"), "toml"));

        assertTrue(thrown.getMessage().contains("toml"), thrown.getMessage());
    }

    @Test
    void malformedContentFailsInsteadOfReturningAnEmptyConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reader.read(streamOf("{esto no es json"), JSON));
    }

    @Test
    void nullDocumentsAndNullRuleValuesHaveConfigurationDiagnostics() {
        for (String content : List.of("null", "{\"enabled\":null}")) {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> reader.read(streamOf(content), JSON));
            assertTrue(exception.getMessage().contains("configuración"));
        }
    }

    private static final class TrackedInput extends ByteArrayInputStream {
        private boolean closed;

        TrackedInput(String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    void bothMappersLeaveBorrowedStreamsOpen() throws IOException {
        for (String format : List.of(JSON, YAML)) {
            try (TrackedInput source = new TrackedInput("{}")) {
                reader.read(source, format);
                assertFalse(source.closed);
            }
        }
    }
}
