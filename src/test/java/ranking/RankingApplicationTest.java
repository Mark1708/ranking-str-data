package ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ranking.cli.CommandLineOptions;

class RankingApplicationTest {
    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("run writes RankedData.csv matching the checked-in golden sample")
    void run_sampleData_writesGoldenRankedData() throws IOException {
        Path input = tempDir.resolve("DataSet.csv");
        Files.copy(Path.of("assets", "DataSet.csv"), input);

        new RankingApplication().run(new CommandLineOptions(input.toString(), "133381", 31, null));

        String generated = Files.readString(tempDir.resolve("RankedData.csv"));
        String expected = Files.readString(Path.of("assets", "RankedData.csv"));
        assertCsvMatchesGolden(generated, expected);
    }

    private void assertCsvMatchesGolden(String generated, String expected) {
        List<String> generatedLines = generated.lines().collect(Collectors.toList());
        List<String> expectedLines = expected.lines().collect(Collectors.toList());
        assertThat(generatedLines).hasSameSizeAs(expectedLines);
        assertHeaderContainsGoldenColumnsAndMetrics(generatedLines.get(0), expectedLines.get(0));

        for (int i = 1; i < expectedLines.size(); i++) {
            assertRowMatchesGolden(generatedLines.get(i), expectedLines.get(i), i + 1);
        }
    }

    private void assertHeaderContainsGoldenColumnsAndMetrics(String generated, String expected) {
        List<String> generatedCells = splitRow(generated);
        List<String> expectedCells = splitRow(expected);

        assertThat(generatedCells.subList(0, expectedCells.size())).isEqualTo(expectedCells);
        assertThat(generatedCells.subList(expectedCells.size(), generatedCells.size()))
                .containsExactly(
                        "TMRCA", "Average number of actual mutations(lambda)", "Average number of mutation steps(k)");
    }

    private void assertRowMatchesGolden(String generated, String expected, int lineNumber) {
        List<String> generatedCells = splitRow(generated);
        List<String> expectedCells = splitRow(expected);
        assertThat(generatedCells).hasSameSizeAs(expectedCells);

        int metricStart = expectedCells.size() - 3;
        assertThat(generatedCells.subList(0, metricStart))
                .describedAs("line %s non-metric cells", lineNumber)
                .isEqualTo(expectedCells.subList(0, metricStart));
        assertMetricCellsAreNumeric(generatedCells, metricStart, lineNumber);
    }

    private void assertMetricCellsAreNumeric(List<String> generatedCells, int metricStart, int lineNumber) {
        for (int i = metricStart; i < generatedCells.size(); i++) {
            assertThat(Double.parseDouble(generatedCells.get(i)))
                    .describedAs("line %s metric column %s", lineNumber, i + 1)
                    .isNotNaN();
        }
    }

    private List<String> splitRow(String row) {
        List<String> cells = Arrays.asList(row.split(";", -1));
        int lastIndex = cells.size() - 1;
        if (lastIndex >= 0 && cells.get(lastIndex).isEmpty()) {
            return cells.subList(0, lastIndex);
        }
        return cells;
    }
}
