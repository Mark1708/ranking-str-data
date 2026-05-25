package ranking.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InputValidatorTest {
    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("validate accepts an existing readable CSV path and positive age")
    void validate_validOptions_returnsSameOptions() throws IOException {
        Path input = Files.createFile(tempDir.resolve("DataSet.csv"));
        CommandLineOptions options = new CommandLineOptions(input.toString(), "133381", 30, null);

        CommandLineOptions result = InputValidator.validate(options);

        assertThat(result).isSameAs(options);
    }

    @Test
    @DisplayName("validate rejects non-positive average age")
    void validate_nonPositiveAge_throws() throws IOException {
        Path input = Files.createFile(tempDir.resolve("DataSet.csv"));
        CommandLineOptions options = new CommandLineOptions(input.toString(), "133381", 0, null);

        assertThatThrownBy(() -> InputValidator.validate(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Average age");
    }

    @Test
    @DisplayName("validate rejects non-positive mutation rate")
    void validate_nonPositiveMutationRate_throws() throws IOException {
        Path input = Files.createFile(tempDir.resolve("DataSet.csv"));
        CommandLineOptions options = new CommandLineOptions(input.toString(), "133381", 30, -0.1);

        assertThatThrownBy(() -> InputValidator.validate(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mutation rate must be positive");
    }

    @Test
    @DisplayName("validate rejects missing CSV files")
    void validate_missingPath_throws() {
        CommandLineOptions options =
                new CommandLineOptions(tempDir.resolve("missing.csv").toString(), "133381", 30, null);

        assertThatThrownBy(() -> InputValidator.validate(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("validate rejects paths that are not readable regular files")
    void validate_directoryPath_throws() {
        CommandLineOptions options = new CommandLineOptions(tempDir.toString(), "133381", 30, null);

        assertThatThrownBy(() -> InputValidator.validate(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not readable");
    }

    @ParameterizedTest(name = "path={0}, index={1}, age={2}, rate={3} is invalid")
    @CsvSource(
            delimiter = '|',
            nullValues = "NULL",
            value = {
                "NULL | 1 | 30 | NULL",
                "'' | 1 | 30 | NULL",
                "EXISTING | NULL | 30 | NULL",
                "EXISTING | '' | 30 | NULL",
                "EXISTING | abc | 0 | NULL",
                "EXISTING | 1 | -1 | NULL",
                "EXISTING | 1 | 30 | -0.001",
                "EXISTING | 1 | 30 | 0.0"
            })
    @DisplayName("validate rejects invalid command line options")
    void validate_invalidInputs_throwsException(
            String pathToken, String haplotypeIndex, int averageAge, Double mutationRate) throws IOException {
        CommandLineOptions options =
                new CommandLineOptions(resolvePath(pathToken), haplotypeIndex, averageAge, mutationRate);

        assertThatThrownBy(() -> InputValidator.validate(options)).isInstanceOf(IllegalArgumentException.class);
    }

    private String resolvePath(String pathToken) throws IOException {
        if (pathToken == null || !pathToken.equals("EXISTING")) {
            return pathToken;
        }
        return Files.createTempFile(tempDir, "DataSet", ".csv").toString();
    }
}
