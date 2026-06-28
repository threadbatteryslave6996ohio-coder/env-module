package dev.clippy.utils.envmanager;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvSchemaTest {
    @Test
    void parsesRequiredAndOptionalValues() {
        EnvClassBuilder builder = EnvSchema.builder();
        EnvOption<String> host = builder.required("APP_HOST", EnvType.string());
        EnvOption<Integer> port = builder.optional("APP_PORT", EnvType.integer(), 8080);
        EnvOption<Boolean> enabled = builder.optional("APP_ENABLED", EnvType.bool());

        Env env = builder.build().from(Map.of(
                "APP_HOST", "localhost",
                "APP_PORT", "9000",
                "APP_ENABLED", "yes"
        ));

        assertEquals("localhost", env.get(host));
        assertEquals(9000, env.get(port));
        assertTrue(env.get(enabled));
    }

    @Test
    void appliesDefaultValuesWhenOptionalValuesAreMissing() {
        EnvClassBuilder builder = EnvSchema.builder();
        EnvOption<String> host = builder.required("APP_HOST", EnvType.string());
        EnvOption<Integer> port = builder.optional("APP_PORT", EnvType.integer(), 8080);
        EnvOption<Boolean> enabled = builder.optional("APP_ENABLED", EnvType.bool());

        Env env = builder.build().from(Map.of("APP_HOST", "localhost"));

        assertEquals("localhost", env.get(host));
        assertEquals(8080, env.get(port));
        assertFalse(env.has(enabled));
        assertThrows(IllegalStateException.class, () -> env.get(enabled));
    }

    @Test
    void rejectsMissingRequiredValues() {
        EnvClassBuilder builder = EnvSchema.builder();
        builder.required("APP_HOST", EnvType.string());

        EnvValidationException exception = assertThrows(
                EnvValidationException.class,
                () -> builder.build().from(Map.of())
        );

        assertEquals("Missing required environment variable: APP_HOST", exception.getMessage());
    }

    @Test
    void rejectsInvalidTypedValues() {
        EnvClassBuilder builder = EnvSchema.builder();
        builder.required("APP_PORT", EnvType.integer());

        EnvValidationException exception = assertThrows(
                EnvValidationException.class,
                () -> builder.build().from(Map.of("APP_PORT", "abc"))
        );

        assertEquals("Invalid environment variable APP_PORT: expected integer", exception.getMessage());
    }

    @Test
    void rejectsOptionsFromDifferentSchemas() {
        EnvClassBuilder firstBuilder = EnvSchema.builder();
        EnvOption<String> firstHost = firstBuilder.required("APP_HOST", EnvType.string());
        Env env = firstBuilder.build().from(Map.of("APP_HOST", "localhost"));

        EnvClassBuilder secondBuilder = EnvSchema.builder();
        EnvOption<String> secondHost = secondBuilder.required("APP_HOST", EnvType.string());
        secondBuilder.build();

        assertEquals("localhost", env.get(firstHost));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> env.get(secondHost));
        assertEquals("Environment option is not part of this schema: APP_HOST", exception.getMessage());
    }

    @Test
    void treatsBlankValuesAsMissingAndFallsBackToDefaults() {
        EnvClassBuilder builder = EnvSchema.builder();
        EnvOption<String> host = builder.required("APP_HOST", EnvType.string());
        EnvOption<Integer> port = builder.optional("APP_PORT", EnvType.integer(), 8080);
        EnvOption<Boolean> enabled = builder.optional("APP_ENABLED", EnvType.bool());

        Env env = builder.build().from(Map.of(
                "APP_HOST", "service.internal",
                "APP_PORT", "   ",
                "APP_ENABLED", ""
        ));

        assertEquals("service.internal", env.get(host));
        assertEquals(8080, env.get(port));
        assertFalse(env.has(enabled));
    }

    @Test
    void rejectsBlankValueForRequiredOption() {
        EnvClassBuilder builder = EnvSchema.builder();
        builder.required("APP_HOST", EnvType.string());

        EnvValidationException exception = assertThrows(
                EnvValidationException.class,
                () -> builder.build().from(Map.of("APP_HOST", "   "))
        );

        assertEquals("Missing required environment variable: APP_HOST", exception.getMessage());
    }

    @Test
    void supportsBooleanAliasesAndWhitespace() {
        EnvClassBuilder builder = EnvSchema.builder();
        EnvOption<Boolean> enabled = builder.required("APP_ENABLED", EnvType.bool());
        EnvOption<Boolean> disabled = builder.required("APP_DISABLED", EnvType.bool());

        Env env = builder.build().from(Map.of(
                "APP_ENABLED", "  ON ",
                "APP_DISABLED", "\tno\n"
        ));

        assertTrue(env.get(enabled));
        assertFalse(env.get(disabled));
    }

    @Test
    void parsesLongValues() {
        EnvClassBuilder builder = EnvSchema.builder();
        EnvOption<Long> timeout = builder.required("APP_TIMEOUT_MS", EnvType.longInteger());

        Env env = builder.build().from(Map.of("APP_TIMEOUT_MS", "922337203685477580"));

        assertEquals(922337203685477580L, env.get(timeout));
    }

    @Test
    void preservesParserCauseForValidationErrors() {
        EnvClassBuilder builder = EnvSchema.builder();
        builder.required("APP_ENABLED", EnvType.bool());

        EnvValidationException exception = assertThrows(
                EnvValidationException.class,
                () -> builder.build().from(Map.of("APP_ENABLED", "sometimes"))
        );

        assertEquals("Invalid environment variable APP_ENABLED: expected boolean", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Invalid boolean value: sometimes", exception.getCause().getMessage());
    }

    @Test
    void rejectsDuplicateOptionNames() {
        EnvClassBuilder builder = EnvSchema.builder();
        builder.required("APP_HOST", EnvType.string());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.optional("APP_HOST", EnvType.integer())
        );

        assertEquals("Duplicate environment option: APP_HOST", exception.getMessage());
    }

    @Test
    void trimsOptionNamesAndExposesDefaultValueMetadata() {
        EnvOption<Integer> option = EnvOption.optional(" APP_PORT ", EnvType.integer(), 8080);

        assertEquals("APP_PORT", option.name());
        assertEquals(Optional.of(8080), option.defaultValue());
    }

    @Test
    void rejectsBlankOptionNames() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EnvOption.required("   ", EnvType.string())
        );

        assertEquals("Environment option name cannot be blank", exception.getMessage());
    }
}
