package ranking.spark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ranking.cli.CommandLineOptions;
import ranking.domain.RankedEntry;

class HaplotypeRankerTest {
    private JavaSparkContext context;
    private SparkSession spark;

    @BeforeEach
    void setUp() {
        SparkConf conf = new SparkConf()
                .setMaster("local[1]")
                .setAppName("RankingTest")
                .set("spark.user", System.getProperty("user.name", "ranking"))
                .set("spark.driver.bindAddress", "127.0.0.1")
                .set("spark.driver.host", "127.0.0.1")
                .set("spark.ui.enabled", "false");
        context = new JavaSparkContext(conf);
        spark = SparkSession.builder().config(conf).getOrCreate();
    }

    @AfterEach
    void tearDown() {
        if (spark != null) {
            spark.stop();
        }
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("rank rejects CSV data without Index as the first column")
    void rank_missingIndexColumn_throws() {
        var schema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("Sample", DataTypes.StringType, true),
                DataTypes.createStructField("DYS393", DataTypes.StringType, true)));
        var data = spark.createDataFrame(List.of(RowFactory.create("base", "1")), schema);

        assertThatThrownBy(() -> new HaplotypeRanker().rank(data, new CommandLineOptions("unused.csv", "base", 30, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first column must be named Index");
    }

    @Test
    @DisplayName("rank rejects a missing base haplotype")
    void rank_missingBaseHaplotype_throws() {
        var schema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("Index", DataTypes.StringType, true),
                DataTypes.createStructField("DYS393", DataTypes.StringType, true)));
        var data = spark.createDataFrame(List.of(RowFactory.create("base", "1")), schema);

        assertThatThrownBy(() -> new HaplotypeRanker().rank(data, new CommandLineOptions("unused.csv", "missing", 30, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base haplotype not found");
    }

    @Test
    @DisplayName("rank with synthetic data produces correct order")
    void rank_syntheticData_ordersByLowestTmrcaFirst() {
        Dataset<Row> data = createDataFrame(
                RowFactory.create("far", "12", "10"),
                RowFactory.create("base", "10", "10"),
                RowFactory.create("near", "11", "10"),
                RowFactory.create("same", "10", "10"));

        List<RankedEntry> result = new HaplotypeRanker().rank(data, new CommandLineOptions("unused.csv", "base", 30, null));

        assertThat(result).extracting(RankedEntry::index).containsExactly("base", "same", "near", "far");
        assertThat(result.get(0).metrics().tmrca()).isZero();
        assertThat(result.get(2).metrics().tmrca()).isLessThan(result.get(3).metrics().tmrca());
    }

    @Test
    @DisplayName("rank rejects duplicate Index values")
    void rank_duplicateIndex_throws() {
        Dataset<Row> data = createDataFrame(RowFactory.create("base", "10", "10"), RowFactory.create("base", "11", "10"));

        assertThatThrownBy(() -> new HaplotypeRanker().rank(data, new CommandLineOptions("unused.csv", "base", 30, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate Index values found")
                .hasMessageContaining("base");
    }

    @Test
    @DisplayName("rank rejects duplicate base haplotype")
    void rank_duplicateBaseHaplotype_throws() {
        Dataset<Row> data = createDataFrame(RowFactory.create("base", "10", "10"), RowFactory.create("base", "10", "10"));

        assertThatThrownBy(() -> new HaplotypeRanker().rank(data, new CommandLineOptions("unused.csv", "base", 30, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate Index values found")
                .hasMessageContaining("base");
    }

    @Test
    @DisplayName("rank handles null locus values gracefully, skipping them pairwise")
    void rank_nullLoci_skipped() {
        Dataset<Row> data = createDataFrame(
                RowFactory.create("base", "10", null, "20"), RowFactory.create("row", "11", "999", null));

        List<RankedEntry> result = new HaplotypeRanker().rank(data, new CommandLineOptions("unused.csv", "base", 30, null));

        assertThat(result).hasSize(2);
    }

    private Dataset<Row> createDataFrame(Row... rows) {
        int columnCount = Arrays.stream(rows).mapToInt(Row::length).max().orElse(1);
        List<org.apache.spark.sql.types.StructField> fields = new ArrayList<>();
        fields.add(DataTypes.createStructField("Index", DataTypes.StringType, true));
        for (int i = 1; i < columnCount; i++) {
            fields.add(DataTypes.createStructField("DYS" + i, DataTypes.StringType, true));
        }
        StructType schema = DataTypes.createStructType(fields);
        return spark.createDataFrame(Arrays.asList(rows), schema);
    }
}
