package ranking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HaplotypeTest {
    @Test
    @DisplayName("constructor rejects null index")
    void constructor_nullIndex_throws() {
        assertThatThrownBy(() -> new Haplotype(null, List.of("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("index");
    }

    @Test
    @DisplayName("constructor rejects blank index")
    void constructor_blankIndex_throws() {
        assertThatThrownBy(() -> new Haplotype(" ", List.of("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("index");
    }

    @Test
    @DisplayName("constructor makes defensive copy of loci list")
    void constructor_lociList_makesDefensiveCopy() {
        List<String> loci = new ArrayList<>(List.of("10", "11"));

        Haplotype haplotype = new Haplotype("base", loci);
        loci.add("12");

        assertThat(haplotype.loci()).containsExactly("10", "11");
        assertThatThrownBy(() -> haplotype.loci().add("13")).isInstanceOf(UnsupportedOperationException.class);
    }
}
