package ranking;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.util.Scanner;
import ranking.cli.CommandLineArgs;
import ranking.cli.CommandLineOptions;
import ranking.cli.InputValidator;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            new RankingApplication().run(readOptions(args));
        } catch (RuntimeException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static CommandLineOptions readOptions(String[] args) {
        if (args.length == 0) {
            return readInteractiveOptions();
        }
        CommandLineArgs parameters = new CommandLineArgs();
        JCommander commander = JCommander.newBuilder().addObject(parameters).build();
        commander.setProgramName("ranking");
        try {
            commander.parse(args);
        } catch (ParameterException ex) {
            commander.usage();
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        if (parameters.isHelp()) {
            commander.usage();
            System.exit(0);
        }
        return InputValidator.validate(parameters.toOptions());
    }

    private static CommandLineOptions readInteractiveOptions() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Path to csv file: ");
            String dataPath = scanner.nextLine();
            System.out.print("Index of haplotype: ");
            String haplotypeIndex = scanner.nextLine();
            System.out.print("Average age: ");
            int averageAge = scanner.nextInt();
            return InputValidator.validate(new CommandLineOptions(dataPath, haplotypeIndex, averageAge, null));
        }
    }
}
