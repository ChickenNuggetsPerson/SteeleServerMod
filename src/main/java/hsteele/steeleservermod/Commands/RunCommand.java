package hsteele.steeleservermod.Commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.Steeleservermod;
import java.io.File;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RunCommand {

    public static String RunFolder = "scripts";
    public static String[] scripts = {
//            "backup.sh",
            "updateMods.sh",
            "restartServer.sh"
    };

    public static LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> register() {

        // Make scripts directory
        File scriptsDir = new File(RunFolder);
        if (!scriptsDir.exists()) {
            boolean created = scriptsDir.mkdirs();
            if (created) {
                Steeleservermod.LOGGER.info("Created scripts directory: {}", scriptsDir.getAbsolutePath());
            } else {
                throw new Error("Error creating Scripts directory");
            }
        }

        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> runCommand = Commands.literal("run")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

        for (String script : scripts) {
            runCommand.then(Commands.literal(script)
                    .executes(ctx -> {
                        return run(ctx, script);
                    }));
        }

        return runCommand;
    }

    private static int run(CommandContext<CommandSourceStack> context, String scriptName) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        File scriptFile = new File(RunFolder, scriptName);

        if (!scriptFile.exists() || !scriptFile.isFile()) {
            source.sendFailure(Component.literal("Script not found: " + scriptFile.getPath()));
            return 0;
        }

        try {
            // Start detached process
            new ProcessBuilder("nohup", "/bin/bash", scriptFile.getAbsolutePath())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectErrorStream(true)
                    .start();

            source.sendSuccess(() -> Component.literal("Started script (detached): " + scriptName), true);
            return 1;

        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to start script: " + e.getMessage()));
            return -1;
        }
    }

    public static void runRestartScript() {
        File scriptFile = new File(RunFolder, "restartServer.sh");
        if (!scriptFile.exists() || !scriptFile.isFile()) {
            return;
        }

        try {
            // Start detached process
            new ProcessBuilder("nohup", "/bin/bash", scriptFile.getAbsolutePath())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException ignored) {

        }
    }

}
