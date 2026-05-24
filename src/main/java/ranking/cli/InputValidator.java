package ranking.cli;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InputValidator {
    private InputValidator() {}

    public static CommandLineOptions validate(CommandLineOptions options) {
        if (options.dataPath() == null || options.dataPath().isBlank()) {
            throw new IllegalArgumentException("CSV path is required");
        }
        if (options.haplotypeIndex() == null || options.haplotypeIndex().isBlank()) {
            throw new IllegalArgumentException("Haplotype index is required");
        }
        if (options.averageAge() <= 0) {
            throw new IllegalArgumentException("Average age must be greater than 0");
        }
        if (options.mutationRate() != null && options.mutationRate() <= 0) {
            throw new IllegalArgumentException("Mutation rate must be positive");
        }
        Path path = Path.of(options.dataPath());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("CSV file does not exist: " + options.dataPath());
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("CSV file is not readable: " + options.dataPath());
        }
        return options;
    }
}
