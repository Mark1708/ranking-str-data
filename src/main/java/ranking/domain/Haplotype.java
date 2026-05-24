package ranking.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record Haplotype(String index, List<String> loci) {
    public Haplotype {
        if (index == null || index.isBlank()) {
            throw new IllegalArgumentException("Haplotype index must not be null or blank");
        }
        loci = Collections.unmodifiableList(new ArrayList<>(loci));
    }
}
