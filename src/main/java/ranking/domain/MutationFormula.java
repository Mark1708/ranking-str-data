package ranking.domain;

import java.util.List;

public interface MutationFormula {
    MutationMetrics calculate(List<String> base, List<String> row, int averageAge);
}
