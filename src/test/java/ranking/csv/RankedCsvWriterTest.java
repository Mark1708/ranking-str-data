package ranking.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RankedCsvWriterTest {
    @TempDir
    private Path tempDir;

    private final RankedCsvWriter writer = new RankedCsvWriter();

    @Test
    @DisplayName("write produces header with metric columns")
    void write_includesMetricColumnsInHeader() throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(row("base", "10")));

        List<String> header = splitCsvLine(Files.readAllLines(output).get(0));

        assertThat(header).containsExactly(
                "Index",
                "DYS393",
                "TMRCA",
                "Average number of actual mutations(lambda)",
                "Average number of mutation steps(k)");
    }

    @Test
    @DisplayName("write produces rows with same number of cells as header")
    void write_rowsHaveSameCellCountAsHeader() throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(row("base", "10"), row("near", "11")));

        List<List<String>> lines = Files.readAllLines(output).stream().map(this::splitCsvLine).toList();

        assertThat(lines.subList(1, lines.size())).allSatisfy(row -> assertThat(row).hasSameSizeAs(lines.get(0)));
    }

    @Test
    @DisplayName("write escapes semicolons in values")
    void write_valueWithSemicolon_wrapsInQuotes() throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(row("base;sample", "10")));

        assertThat(Files.readAllLines(output).get(1)).startsWith("\"base;sample\";10;");
    }

    @Test
    @DisplayName("write escapes quotes in values")
    void write_valueWithQuote_doublesQuoteAndWrapsInQuotes() throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(row("base\"sample", "10")));

        assertThat(Files.readAllLines(output).get(1)).startsWith("\"base\"\"sample\";10;");
    }

    @Test
    @DisplayName("write handles null values")
    void write_nullValue_writesSpace() throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(row("base", null)));

        assertThat(Files.readAllLines(output).get(1)).startsWith("base; ;");
    }

    @Test
    @DisplayName("write appends trailing semicolons")
    void write_linesEndWithTrailingSemicolon() throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(row("base", "10"), row("near", "11")));

        assertThat(Files.readAllLines(output)).allSatisfy(line -> assertThat(line).endsWith(";"));
    }

    @ParameterizedTest(name = "tmrca={0}, lambda={1}, k={2}")
    @CsvSource({
        "0.0, 0.0, 0.0",
        "107.415, 0.009, 0.00897",
        "2148.3, 0.18, 0.165",
        "2884.615385, 0.25, 0.22235",
        "39038.461538, 2.9, 1.529784"
    })
    @DisplayName("write outputs rows with various mutation metrics")
    void writeRow_variousMetrics_correctCsvOutput(double tmrca, double lambda, double k) throws IOException {
        Path output = writer.write(inputPath(), headers(), List.of(rowWithMetrics(tmrca, lambda, k)));

        List<String> cells = splitCsvLine(Files.readAllLines(output).get(1));

        assertThat(cells).containsExactly(
                "base", "10", Double.toString(tmrca), Double.toString(lambda), Double.toString(k));
    }

    private String inputPath() {
        return tempDir.resolve("DataSet.csv").toString();
    }

    private List<String> headers() {
        return List.of(
                "Index",
                "DYS393",
                "TMRCA",
                "Average number of actual mutations(lambda)",
                "Average number of mutation steps(k)");
    }

    private List<String> row(String index, String locus) {
        return Arrays.asList(index, locus, "0.0", "0.0", "0.0");
    }

    private List<String> rowWithMetrics(double tmrca, double lambda, double k) {
        return Arrays.asList("base", "10", Double.toString(tmrca), Double.toString(lambda), Double.toString(k));
    }

    private List<String> splitCsvLine(String line) {
        String[] cells = line.split(";", -1);
        return List.of(cells).subList(0, cells.length - 1);
    }
}
