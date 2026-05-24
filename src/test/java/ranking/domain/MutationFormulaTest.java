package ranking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MutationFormulaTest {
    @Test
    @DisplayName("MutationCalculator implements MutationFormula")
    void mutationCalculator_implementsMutationFormula() {
        assertThat(new MutationCalculator()).isInstanceOf(MutationFormula.class);
    }

    @Test
    @DisplayName("MutationFormula supports polymorphic calculate calls")
    void calculate_throughInterface_returnsMetrics() {
        MutationFormula formula = new MutationCalculator();

        MutationMetrics metrics = formula.calculate(List.of("10", "10"), List.of("11", "10"), 30);

        double expectedTmrca = 30 * ((0.5 / 2) / MutationCalculator.DEFAULT_MUTATION_RATE);
        assertThat(metrics.tmrca()).isEqualTo(expectedTmrca);
    }

    @ParameterizedTest(name = "lambda={0} -> k={1}")
    @CsvSource({
        "0.0, 0.0",
        "0.01, 0.009950",
        "0.1, 0.095242",
        "1.0, 0.683940",
        "2.0, 1.135335",
        "5.0, 2.516845",
        "10.0, 5.000227"
    })
    @DisplayName("Klyosov forward formula computes observed mutation steps")
    void klyosovForwardFormula(double lambda, double expectedK) {
        double actualK = (lambda / 2) * (1 + Math.exp(-lambda));

        assertEquals(expectedK, actualK, 0.0001);
    }

    @ParameterizedTest(name = "rate={0}, tmrcaGenerations={1} -> lambda={2}")
    @CsvSource({
        "0.0026, 0.0, 0.0",
        "0.0026, 96.153846, 0.25",
        "0.0026, 384.615385, 1.0",
        "0.01, 100.0, 1.0",
        "0.004, 250.0, 1.0",
        "0.001, 1500.0, 1.5"
    })
    @DisplayName("lambda is mutation rate multiplied by TMRCA generations")
    void lambda_usesMutationRateTimesTmrcaGenerations(
            double mutationRate, double tmrcaGenerations, double expectedLambda) {
        double actualLambda = mutationRate * tmrcaGenerations;

        assertEquals(expectedLambda, actualLambda, 0.001);
    }
}
