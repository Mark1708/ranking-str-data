package ranking.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class RankedCsvWriter {
    private static final String OUTPUT_FILE_NAME = "RankedData.csv";
    public static final List<String> METRIC_COLUMNS = List.of(
            "TMRCA", "Average number of actual mutations(lambda)", "Average number of mutation steps(k)");

    public Path write(String inputPath, List<String> headers, List<List<String>> rows) {
        Path outputPath = outputPathFor(inputPath);
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write(escapeAndJoin(headers));
            writer.write("\n");
            for (List<String> row : rows) {
                writer.write(escapeAndJoin(row));
                writer.write("\n");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write ranked CSV: " + outputPath, ex);
        }
        return outputPath;
    }

    public Path outputPathFor(String inputPath) {
        Path parent = Path.of(inputPath).toAbsolutePath().getParent();
        if (parent == null) {
            return Path.of(OUTPUT_FILE_NAME).toAbsolutePath();
        }
        return parent.resolve(OUTPUT_FILE_NAME);
    }

    private String escapeAndJoin(List<String> values) {
        return values.stream().map(this::escapeCsvValue).collect(Collectors.joining(";")) + ";";
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return " ";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
