package ranking.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record RankedEntry(String index, List<String> locusValues, MutationMetrics metrics) {
    public RankedEntry {
        locusValues = Collections.unmodifiableList(new ArrayList<>(locusValues));
    }
}
