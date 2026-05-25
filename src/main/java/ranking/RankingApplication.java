package ranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import ranking.cli.CommandLineOptions;
import ranking.csv.RankedCsvWriter;
import ranking.domain.MutationCalculator;
import ranking.domain.RankedEntry;
import ranking.spark.HaplotypeRanker;

public final class RankingApplication {
    public void run(CommandLineOptions options) {
        SparkConf conf = new SparkConf()
                .setMaster("local[1]")
                .setAppName("Ranking")
                .set("spark.user", System.getProperty("user.name", "ranking"))
                .set("spark.driver.bindAddress", "127.0.0.1")
                .set("spark.driver.host", "127.0.0.1")
                .set("spark.ui.enabled", "false");
        try (JavaSparkContext context = new JavaSparkContext(conf)) {
            SparkSession spark = SparkSession.builder().config(conf).getOrCreate();
            try {
                Dataset<Row> data = readData(spark, options.dataPath());
                List<String> inputColumns = Arrays.asList(data.columns());
                MutationCalculator calculator = options.mutationRate() != null
                        ? new MutationCalculator(options.mutationRate())
                        : new MutationCalculator();
                List<RankedEntry> rankedEntries = new HaplotypeRanker(calculator).rank(data, options);

                List<String> headers = new ArrayList<>(inputColumns);
                headers.addAll(RankedCsvWriter.METRIC_COLUMNS);

                List<List<String>> rows =
                        rankedEntries.stream().map(this::toOutputRow).collect(Collectors.toUnmodifiableList());

                new RankedCsvWriter().write(options.dataPath(), headers, rows);
                printReport(data, rankedEntries, options.haplotypeIndex());
            } finally {
                spark.stop();
            }
        }
    }

    private Dataset<Row> readData(SparkSession spark, String dataPath) {
        return spark.read()
                .format("csv")
                .option("sep", ";")
                .option("header", "true")
                .load(dataPath);
    }

    private List<String> toOutputRow(RankedEntry entry) {
        List<String> row = new ArrayList<>();
        row.add(entry.index());
        row.addAll(entry.locusValues());
        row.add(String.valueOf(entry.metrics().tmrca()));
        row.add(String.valueOf(entry.metrics().actualMutations()));
        row.add(String.valueOf(entry.metrics().observedMutations()));
        return List.copyOf(row);
    }

    private void printReport(Dataset<Row> data, List<RankedEntry> rankedEntries, String haplotypeIndex) {
        System.out.println("Size of processing data: " + data.columns().length + "x" + rankedEntries.size());
        System.out.println("\nData info: ");
        data.drop("Index").describe().show(6, false);
        System.out.println("\nThe haplotype relative to which the STR data will be ranked: ");
        data.filter(org.apache.spark.sql.functions.col("Index").equalTo(haplotypeIndex))
                .show(1, false);
        System.out.println("\nRanked data (top 10): ");
        rankedEntries.stream()
                .limit(10)
                .forEach(entry -> System.out.printf(
                        "  %s: TMRCA=%.6f, lambda=%.6f, k=%.6f%n",
                        entry.index(),
                        entry.metrics().tmrca(),
                        entry.metrics().actualMutations(),
                        entry.metrics().observedMutations()));
    }
}
