package ranking.cli;

import com.beust.jcommander.Parameter;

public final class CommandLineArgs {
    @Parameter(
            names = {"-p", "--path"},
            description = "Path to csv file",
            required = true)
    private String dataPath;

    @Parameter(
            names = {"-i", "--index"},
            description = "Index of haplotype",
            required = true)
    private String haplotypeIndex;

    @Parameter(
            names = {"-a", "--age"},
            description = "Average age",
            required = true)
    private int averageAge;

    @Parameter(
            names = {"--mu"},
            description = "Mutation rate per locus per generation (default: 0.0026)")
    private Double mutationRate;

    @Parameter(
            names = {"-h", "--help"},
            description = "Help/Usage",
            help = true)
    private boolean help;

    public boolean isHelp() {
        return help;
    }

    public CommandLineOptions toOptions() {
        return new CommandLineOptions(dataPath, haplotypeIndex, averageAge, mutationRate);
    }
}
