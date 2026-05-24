package ranking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MutationCalculatorTest {
    private final MutationCalculator calculator = new MutationCalculator();

    @Test
    @DisplayName("calculate preserves TMRCA and computes lambda before observed k")
    void calculate_validLoci_usesOriginalFormulas() {
        MutationMetrics result = calculator.calculate(Arrays.asList("1", "2", "5"), Arrays.asList("2", "4", "5"), 30);

        double sum = Math.pow(1 - 2, 2) / 2 + Math.pow(2 - 4, 2) / 2 + Math.pow(5 - 5, 2) / 2;
        double lambda = sum / 3;
        double tmrca = 30 * (lambda / MutationCalculator.DEFAULT_MUTATION_RATE);
        double k = (lambda / 2) * (1 + Math.exp(-lambda));

        assertThat(result.tmrca()).isEqualTo(tmrca);
        assertThat(result.actualMutations()).isEqualTo(lambda);
        assertThat(result.observedMutations()).isEqualTo(k);
    }

    @ParameterizedTest(name = "loci={0}/{1}, age={2} -> tmrca={3}, lambda={4}, k={5}")
    @CsvSource(
            delimiter = '|',
            value = {
                "13,13,13 | 13,13,13 | 30 | 0.0 | 0.0 | 0.0",
                "10,10 | 11,10 | 30 | 2884.615385 | 0.25 | 0.222350",
                "10,10 | 12,10 | 25 | 9615.384615 | 1.0 | 0.683940",
                "10,11,12 | 11,13,12 | 30 | 9615.384615 | 0.833333 | 0.597749",
                "8,9,10,11 | 10,9,9,14 | 20 | 13461.538462 | 1.75 | 1.027052",
                "15,16,17,18,19 | 15,18,20,18,23 | 35 | 39038.461538 | 2.9 | 1.529784"
            })
    @DisplayName("calculate returns expected metrics for multiple locus differences")
    void calculate_parameterizedCases(
            String refLoci,
            String compLoci,
            int age,
            double expectedTmrca,
            double expectedLambda,
            double expectedK) {
        MutationMetrics result = calculator.calculate(loci(refLoci), loci(compLoci), age);

        assertEquals(expectedTmrca, result.tmrca(), 0.001);
        assertEquals(expectedLambda, result.actualMutations(), 0.001);
        assertEquals(expectedK, result.observedMutations(), 0.001);
    }

    @Test
    @DisplayName("constructor rejects non-positive mutation rate")
    void constructor_nonPositiveMutationRate_throws() {
        assertThatThrownBy(() -> new MutationCalculator(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mutation rate must be positive");
    }

    @Test
    @DisplayName("calculate rejects rows without comparable loci")
    void calculate_noComparableLoci_throws() {
        assertThatThrownBy(() -> calculator.calculate(Arrays.asList(null, null), Arrays.asList("1", null), 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No comparable loci");
    }

    @Test
    @DisplayName("calculate rejects invalid integer locus values")
    void calculate_invalidInteger_throws() {
        assertThatThrownBy(() -> calculator.calculate(Arrays.asList("1"), Arrays.asList("bad"), 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid integer locus value");
    }

    private List<String> loci(String values) {
        return Arrays.asList(values.split(","));
    }
}
