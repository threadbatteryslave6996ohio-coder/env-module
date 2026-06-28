package dev.clippy.utils.envmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsDotenvFileFromProvidedDirectory() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                APP_HOST=localhost
                APP_PORT=8080
                APP_ENABLED="true"
                # ignored comment

                INVALID_LINE
                """);

        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertEquals("localhost", values.get("APP_HOST"));
        assertEquals("8080", values.get("APP_PORT"));
        assertEquals("true", values.get("APP_ENABLED"));
        assertEquals(3, values.size());
    }

    @Test
    void searchesParentDirectoriesForDotenvFile() throws IOException {
        Path nestedDirectory = Files.createDirectories(tempDir.resolve("a/b/c"));
        Files.writeString(tempDir.resolve(".env"), "APP_NAME=env-module\n");

        Map<String, String> values = EnvFiles.loadDotenvOnly(nestedDirectory);

        assertEquals(Map.of("APP_NAME", "env-module"), values);
    }

    @Test
    void loadsAnExplicitFilePath() throws IOException {
        Path dotenv = tempDir.resolve("custom.env");
        Files.writeString(dotenv, "APP_SECRET='top-secret'\n");

        Map<String, String> values = EnvFiles.loadFile(dotenv);

        assertEquals(Map.of("APP_SECRET", "top-secret"), values);
    }

    @Test
    void returnsEmptyMapWhenDotenvFileDoesNotExist() throws IOException {
        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertTrue(values.isEmpty());
    }

    @Test
    void laterDuplicateKeysOverrideEarlierOnes() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                APP_HOST=localhost
                APP_HOST=api.internal
                """);

        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertEquals(Map.of("APP_HOST", "api.internal"), values);
    }

    @Test
    void keepsEverythingAfterFirstEqualsInValue() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "JDBC_URL=jdbc:postgresql://db/service?sslmode=require\n");

        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertEquals("jdbc:postgresql://db/service?sslmode=require", values.get("JDBC_URL"));
    }

    @Test
    void trimsKeysAndUnquotesSingleAndDoubleQuotedValues() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                  APP_NAME = "env module"
                APP_TOKEN='secret-token'
                """);

        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertEquals("env module", values.get("APP_NAME"));
        assertEquals("secret-token", values.get("APP_TOKEN"));
    }

    @Test
    void preservesEmptyAssignedValues() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "EMPTY_VALUE=\n");

        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertTrue(values.containsKey("EMPTY_VALUE"));
        assertEquals("", values.get("EMPTY_VALUE"));
    }

    @Test
    void ignoresEntriesWithBlankKeys() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                =ignored
                   =also-ignored
                APP_NAME=env-module
                """);

        Map<String, String> values = EnvFiles.loadDotenvOnly(tempDir);

        assertEquals(Map.of("APP_NAME", "env-module"), values);
    }

    @Test
    void loadIncludesProcessEnvironmentValues() throws IOException {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "DOTENV_ONLY=value-from-dotenv\n");

        Map<String, String> values = EnvFiles.load(tempDir);

        assertEquals("value-from-dotenv", values.get("DOTENV_ONLY"));
        assertFalse(values.isEmpty());
        assertTrue(values.entrySet().stream().anyMatch(entry -> entry.getValue() != null && !entry.getValue().isBlank()));
    }
}
