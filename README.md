# ranking-str-data

![Java](https://img.shields.io/badge/-Java-0a0a0a?style=for-the-badge&logo=Java) ![Spark](https://img.shields.io/badge/-Apache%20Spark-0a0a0a?style=for-the-badge&logo=Apache%20Spark)

[Русская версия](README.ru.md)

## Goal

CLI tool for evaluating TMRCA (Time to the Most Recent Common Ancestor) by Y-STR loci, then ranking haplotypes relative to a base haplotype. Built for a genetics research group.

## Scope

- Reads CSV input with Y-STR haplotype data
- Calculates genetic distances using Poisson distribution model
- Ranks haplotypes by proximity to a specified base haplotype
- Supports configurable average generation age parameter
- Supports configurable mutation rate per locus per generation (`--mu` flag, default 0.0026)

## Algorithm

The tool implements the Klyosov (2009a) linear method for Y-STR TMRCA estimation with back-mutation correction:

1. **ASD** (Average Squared Distance): `ASD = sum((ref_i - comp_i)^2) / n` where n is the number of matching loci
2. **TMRCA**: `T = averageAge * ASD / mutationRate` (years before present)
3. **Lambda** (actual mutations): `lambda = mutationRate * T_generations` where `T_generations = T / averageAge`
4. **K** (observed mutations, with back-mutation correction): `k = (lambda / 2) * (1 + exp(-lambda))` (Klyosov forward formula)

**Default mutation rate**: 0.0026 per locus per generation (Ballantyne et al. 2010, weighted average across 186 Y-STR markers).

**Output columns**: Index, Loci (matching count), TMRCA (years), Lambda (actual mutations), K (observed mutations with back-mutation correction).

## Reproducibility

**Prerequisites:** Java 21+, Maven

**Build:**

```bash
mvn clean package
```

**Test:**

```bash
mvn clean test
```

**Run:**

```bash
java -jar target/ranking.jar -p /path/to/DataSet.csv -i indexOfHaplotype -a averageAge
```

**With custom mutation rate:**

```bash
java -jar target/ranking.jar -p /path/to/DataSet.csv -i indexOfHaplotype -a averageAge --mu 0.0024
```

**Help:**

```bash
java -jar target/ranking.jar -h
```

**Input format:** CSV where the first column is named "Index", followed by locus names.

Note: The first column should be named "Index", followed by the names of the loci.

## Input Assumptions

- Input data is semicolon-delimited CSV format
- First column must be named "Index"
- Locus values should be integer-like when present (numeric STR values)
- Blank/null values are skipped pairwise during comparison
- Base haplotype specified by index must exist in the input dataset

## Dataset Size

This tool processes semicolon-delimited CSV files with Y-STR haplotype data:

- **Typical input**: 10-500 haplotypes, 20-100 loci per haplotype
- **Memory**: The tool loads all data into memory for metric calculation. For datasets larger than 10,000 rows, ensure sufficient JVM heap (`-Xmx`)
- **Spark mode**: Runs in `local[1]` mode. Spark provides CSV parsing infrastructure; metric calculation is performed in-memory on the driver

If processing very large datasets (>100k rows), consider running on a Spark cluster with the thin-jar profile:

```bash
mvn clean package -Pthin-jar
spark-submit --class ranking.Main target/ranking.jar -p /path/to/data.csv -i index -a 30
```

## Example

- Before ranking: [DataSet](https://github.com/Mark1708/ranking-str-data/blob/main/assets/DataSet.csv)
- After ranking: [RankedDataSet](https://github.com/Mark1708/ranking-str-data/blob/main/assets/RankedData.csv)

## Screenshots

![Example 1](https://github.com/Mark1708/ranking-str-data/blob/main/assets/Exanple1.png?raw=true)
![Example 2](https://github.com/Mark1708/ranking-str-data/blob/main/assets/Exanple2.png?raw=true)
![Example 3](https://github.com/Mark1708/ranking-str-data/blob/main/assets/Exanple3.png?raw=true)

## Limitations

- Research/local CLI tool, not a production pipeline
- Built for Spark 4.1.2 local mode with Log4j 2.26.0
- Sample and golden outputs are behavior references, not production benchmarks
- Genetic distance formulas are not clinically validated for diagnostic purposes
- Formulas should not be modified without domain review and validation
- Requires Java 21+ (LTS)
- All CSV columns are read as strings to preserve data fidelity (leading zeros in Index, exact locus values)
- Back-mutation correction formula assumes low mutation rates; accuracy degrades for lambda > 2

## Background reading

- [TMRCA](https://en.wikipedia.org/wiki/Most_recent_common_ancestor)
- [Poisson distribution](https://en.wikipedia.org/wiki/Poisson_distribution)
- [Y-STR haplotypes](https://en.wikipedia.org/wiki/Haplotype#Y-DNA_haplotypes_from_genealogical_DNA_tests)
- [Klyosov AN. "Haplogroup R1a" (2009a)](https://www.scirp.org/journal/paperinformation?paperid=8736)
- [Ballantyne KN et al. "Mutability of Y-chromosomal microsatellites" (2010)](https://doi.org/10.1016/j.fsigen.2010.03.006)

## Status

Finished research project.

---

By [Mark Gurianov](https://mark1708.github.io/)
