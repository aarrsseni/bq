package io.bootique.tools.shell.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.tools.shell.ConfigService;
import io.bootique.tools.shell.Formatter;
import io.bootique.tools.shell.Shell;
import io.bootique.tools.shell.Toolchain;

public class ConfigCommand extends CommandWithMetadata implements ShellCommand {

    @Inject
    private Provider<ConfigService> configServiceProvider;

    @Inject
    private Shell shell;

    private final Map<String, String> supportedParams;

    public ConfigCommand() {
        super(CommandMetadata.builder("config")
                .description("Read or set global config")
                .addOption(OptionMetadata.builder("param")
                        .description("parameter name, optional")
                        .valueOptional()
                        .build())
                .addOption(OptionMetadata.builder("value")
                        .description("parameter value, optional")
                        .valueOptional()
                        .build())
                .build());

        Map<String, String> params = new HashMap<>();
        params.put(ConfigService.TOOLCHAIN,  "Default toolchain to use. Can be either Maven or Gradle.");
        params.put(ConfigService.BQ_VERSION, "Bootique version to use.");
        params.put(ConfigService.GROUP_ID,   "Default artifact group id to use.");
        supportedParams = Collections.unmodifiableMap(params);
    }

    @Override
    public CommandOutcome run(Cli cli) {

        List<String> args = cli.standaloneArguments();
        String param = null;
        String value = null;
        switch (args.size()) {
            case 2:
                value = args.get(1);
            case 1:
                param = args.get(0);
        }

        ConfigService configService = this.configServiceProvider.get();

        if(param != null) {
            if(value != null) {
                // set
                CommandOutcome outcome = validate(param, value);
                if(!outcome.isSuccess()) {
                    return outcome;
                }
                configService.set(param, value);
            } else {
                // get
                value = configService.get(param);
                if(value == null) {
                    shell.println("No value is set for @|bold " + param + "|@");
                } else {
                    shell.println(param + " = " + value);

                }
            }
        } else {
            // get all
            shell.println("@|underline Available configuration options:|@");
            supportedParams.forEach(this::formatConfigParameter);
        }

        return CommandOutcome.succeeded();
    }

    private CommandOutcome validate(String param, String value) {
        if(!supportedParams.containsKey(param)) {
            return CommandOutcome.failed(-1, "Unsupported option @|bold " + param
                    + "|@. Available parameters: " + String.join(", ", supportedParams.keySet()));
        }

        if(ConfigService.TOOLCHAIN.equals(param)) {
            Toolchain toolchain = Toolchain.byName(value);
            if(toolchain == null) {
                return CommandOutcome.failed(-1, "Unsupported toolchain @|bold " + value
                        + "|@. Supported: " + Arrays.stream(Toolchain.values())
                                                .map(s -> s.name().toLowerCase())
                                                .collect(Collectors.joining(", ")));

            }
        }

        return CommandOutcome.succeeded();
    }

    private void formatConfigParameter(String param, String description) {
        ConfigService configService = this.configServiceProvider.get();
        String value = configService.get(param);

        shell.println("  @|cyan " + Formatter.alignByColumns(param) + "|@"
                + description
                + (value == null ? "" : " Current value: @|bold " + value + " |@"));
    }
}
