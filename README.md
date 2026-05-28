# ranking-str-data

![Java 21](https://img.shields.io/badge/runtime-Java%2021-111827?style=for-the-badge&labelColor=111827&color=5b5ef4) ![Spark](https://img.shields.io/badge/data-Spark%204.1.2-111827?style=for-the-badge&labelColor=111827&color=5b5ef4) ![Maven](https://img.shields.io/badge/build-Maven-111827?style=for-the-badge&labelColor=111827&color=5b5ef4) ![Status](https://img.shields.io/badge/status-research%20CLI-111827?style=for-the-badge&labelColor=111827&color=5b5ef4)

[Русская версия](README.ru.md)

Local Java CLI for ranking Y-STR haplotypes against a selected base haplotype and appending TMRCA-related research metrics to a semicolon-separated CSV file.

| Area | Details |
| --- | --- |
| Runtime | Java 21, Maven |
| Entry point | `ranking.Main` |
| Artifact | Maven `artifactId` `ranking`, final jar `target/ranking.jar` |
| Data engine | Spark 4.1.2 with Scala 2.13, Hadoop 3.5.0 |
| CLI parser | JCommander 1.82 |
| Logging | Log4j 2.26.0 |
| Test and QA setup | JUnit 6.1.0, AssertJ 3.27.7, JaCoCo 0.8.14, SpotBugs, FindSecBugs, Spotless |
| Sample data | `assets/DataSet.csv`, `assets/RankedData.csv` |

## Goal

The project was built for a genetics research workflow. It reads Y-STR haplotype rows, compares each row with a uniquely selected base haplotype, computes research metrics, and writes a ranked CSV next to the input file.

This is a local research tool, not a clinical or diagnostic system.

## Research model

The code implements a linear Y-STR TMRCA calculation based on the Klyosov (2009a) method with back-mutation correction.

1. `ASD = sum((ref_i - comp_i)^2) / n`, where `n` is the number of comparable loci.
2. `TMRCA = averageAge * ASD / mutationRate`, reported in years.
3. `lambda = mutationRate * T_generations`, where `T_generations = TMRCA / averageAge`.
4. `k = (lambda / 2) * (1 + exp(-lambda))`, the Klyosov forward formula for observed mutation steps with back-mutation correction.

The default mutation rate is `0.0026` per locus per generation, based on Ballantyne et al. 2010 as a weighted average across 186 Y-STR markers. It can be changed with `--mu`.

## CLI usage

Build the executable jar:

```bash
mvn clean package
```

Run with required flags:

```bash
java -jar target/ranking.jar -p assets/DataSet.csv -i indexOfHaplotype -a averageAge
```

Run with a custom mutation rate:

```bash
java -jar target/ranking.jar -p assets/DataSet.csv -i indexOfHaplotype -a averageAge --mu 0.0024
```

Show help:

```bash
java -jar target/ranking.jar -h
```

Supported flags:

| Flag | Meaning |
| --- | --- |
| `-p`, `--path` | Path to the input CSV file |
| `-i`, `--index` | `Index` value of the base haplotype |
| `-a`, `--age` | Average generation age used in the TMRCA formula |
| `--mu` | Mutation rate per locus per generation, default `0.0026` |
| `-h`, `--help` | Print CLI help |

## Reproducibility

The expected local build output is:

```text
target/ranking.jar
```

The jar is executable because the Maven assembly configuration points to `ranking.Main`, and the project final name is `ranking`.

The application starts Spark in `local[1]` mode, disables the Spark UI, reads CSV rows through Spark, then loads all rows to the driver for ranking.

Sample files:

- Input sample: [`assets/DataSet.csv`](assets/DataSet.csv)
- Ranked sample: [`assets/RankedData.csv`](assets/RankedData.csv)

Screenshots:

![Example 1](assets/Exanple1.png)
![Example 2](assets/Exanple2.png)
![Example 3](assets/Exanple3.png)

The filenames use the spelling currently present in the repository.

## Inputs and outputs

Input requirements:

- CSV separator is a semicolon.
- The first column must be exactly `Index`.
- `Index` values must be unique. Duplicate values are rejected.
- The base haplotype passed through `-i` or `--index` must exist exactly once.
- Locus values must parse as integers when present.
- Null or blank locus values are skipped pairwise during comparison.

Output behavior:

- The output file is named `RankedData.csv`.
- It is written to the same directory as the input file.
- Existing input columns are preserved.
- The code appends these metrics: `TMRCA`, `Average number of actual mutations(lambda)`, `Average number of mutation steps(k)`.

## Limitations

- The formulas are research assumptions and are not clinically validated for diagnostic use.
- Formula changes should be reviewed by a domain expert.
- Spark is used in local `local[1]` mode for this CLI workflow.
- Ranking loads all rows to the driver, so available JVM memory limits dataset size.
- All CSV columns are read as strings before parsing metrics, which helps preserve exact `Index` values.
- Back-mutation correction assumes low mutation rates. Accuracy degrades for larger `lambda` values.

## References

- [TMRCA](https://en.wikipedia.org/wiki/Most_recent_common_ancestor)
- [Poisson distribution](https://en.wikipedia.org/wiki/Poisson_distribution)
- [Y-STR haplotypes](https://en.wikipedia.org/wiki/Haplotype#Y-DNA_haplotypes_from_genealogical_DNA_tests)
- [Klyosov AN. "Haplogroup R1a" (2009a)](https://www.scirp.org/journal/paperinformation?paperid=8736)
- [Ballantyne KN et al. "Mutability of Y-chromosomal microsatellites" (2010)](https://doi.org/10.1016/j.fsigen.2010.03.006)

## Status

Research/educational project. Results, dependencies, and runtime assumptions are documented for reproducibility, but the repository is not maintained as a packaged product.
