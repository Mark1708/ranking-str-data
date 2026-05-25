package ranking.spark;

import static org.apache.spark.sql.functions.col;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import ranking.cli.CommandLineOptions;
import ranking.domain.MutationCalculator;
import ranking.domain.MutationFormula;
import ranking.domain.MutationMetrics;
import ranking.domain.RankedEntry;

public final class HaplotypeRanker {
    private static final String INDEX_COLUMN = "Index";

    private final MutationFormula calculator;

    public HaplotypeRanker() {
        this(new MutationCalculator());
    }

    public HaplotypeRanker(MutationFormula calculator) {
        this.calculator = calculator;
    }

    public List<RankedEntry> rank(Dataset<Row> data, CommandLineOptions options) {
        validateColumns(data.columns());
        validateNoDuplicateIndices(data);
        Row baseRow = findBaseRow(data, options.haplotypeIndex());
        List<String> baseLoci = toLocusValues(baseRow);

        List<Row> inputRows = data.collectAsList();
        return IntStream.range(0, inputRows.size())
                .mapToObj(i -> Map.entry(i, toRankedEntry(inputRows.get(i), baseLoci, options.averageAge())))
                .sorted(Comparator.<Map.Entry<Integer, RankedEntry>>comparingDouble(
                                e -> e.getValue().metrics().tmrca())
                        .thenComparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toUnmodifiableList());
    }

    private void validateColumns(String[] columns) {
        if (columns.length < 2 || !INDEX_COLUMN.equals(columns[0])) {
            throw new IllegalArgumentException("CSV first column must be named Index");
        }
    }

    private void validateNoDuplicateIndices(Dataset<Row> data) {
        List<Row> duplicates =
                data.groupBy(INDEX_COLUMN).count().filter(col("count").gt(1)).collectAsList();
        if (!duplicates.isEmpty()) {
            String dupIndices = duplicates.stream()
                    .map(row -> row.isNullAt(0) ? "null" : row.getString(0))
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Duplicate Index values found: " + dupIndices);
        }
    }

    private Row findBaseRow(Dataset<Row> data, String haplotypeIndex) {
        List<Row> rows = data.filter(col(INDEX_COLUMN).equalTo(haplotypeIndex)).collectAsList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Base haplotype not found: " + haplotypeIndex);
        }
        if (rows.size() > 1) {
            throw new IllegalArgumentException("Multiple base haplotypes found for index: " + haplotypeIndex);
        }
        return rows.get(0);
    }

    private RankedEntry toRankedEntry(Row row, List<String> baseLoci, int averageAge) {
        MutationMetrics metrics = calculator.calculate(baseLoci, toLocusValues(row), averageAge);
        return new RankedEntry(indexValue(row), toLocusValues(row), metrics);
    }

    private List<String> toLocusValues(Row row) {
        List<String> values = new ArrayList<>();
        for (int i = 1; i < row.length(); i++) {
            values.add(row.isNullAt(i) ? null : String.valueOf(row.get(i)));
        }
        return Collections.unmodifiableList(values);
    }

    private String indexValue(Row row) {
        if (row.isNullAt(0)) {
            throw new IllegalArgumentException("Index value must not be null");
        }
        return String.valueOf(row.get(0));
    }
}
