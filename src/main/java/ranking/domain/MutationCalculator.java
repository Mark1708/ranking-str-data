package ranking.domain;

import java.util.List;

public final class MutationCalculator implements MutationFormula {
    /**
     * Average Y-STR mutation rate per locus per generation (0.0026). Source: Ballantyne et al. (2010)
     * "Mutability of Y-chromosomal microsatellite markers", Forensic Science International: Genetics, 4(2),
     * 81-96. DOI: 10.1016/j.fsigen.2010.03.006
     */
    public static final double DEFAULT_MUTATION_RATE = 0.0026;

    private final double mutationRate;

    /** Creates calculator with default mutation rate (0.0026 per Ballantyne et al. 2010). */
    public MutationCalculator() {
        this(DEFAULT_MUTATION_RATE);
    }

    /**
     * Creates calculator with custom mutation rate.
     *
     * @param mutationRate mutation rate per locus per generation (must be positive)
     * @throws IllegalArgumentException if mutationRate is not positive
     */
    public MutationCalculator(double mutationRate) {
        if (mutationRate <= 0) {
            throw new IllegalArgumentException("Mutation rate must be positive, got: " + mutationRate);
        }
        this.mutationRate = mutationRate;
    }

    public MutationMetrics calculate(List<String> base, List<String> row, int averageAge) {
        if (base.size() != row.size()) {
            throw new IllegalArgumentException("Base and row locus counts must match");
        }
        double sum = 0;
        int notNullLocusCount = 0;
        for (int i = 0; i < row.size(); i++) {
            String baseValue = base.get(i);
            String rowValue = row.get(i);
            if (baseValue != null && rowValue != null) {
                int diff = parseLocus(baseValue, i) - parseLocus(rowValue, i);
                sum += Math.pow(diff, 2) / 2;
                notNullLocusCount++;
            }
        }
        if (notNullLocusCount == 0) {
            throw new IllegalArgumentException("No comparable loci for row");
        }
        double tmrcaGenerations = (sum / notNullLocusCount) / mutationRate;
        double tmrca = averageAge * tmrcaGenerations;
        double actualMutations = mutationRate * tmrcaGenerations;
        double observedMutations = (actualMutations / 2) * (1 + Math.exp(-actualMutations));
        return new MutationMetrics(tmrca, actualMutations, observedMutations);
    }

    private int parseLocus(String value, int position) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid integer locus value at position " + (position + 1) + ": " + value, ex);
        }
    }
}
