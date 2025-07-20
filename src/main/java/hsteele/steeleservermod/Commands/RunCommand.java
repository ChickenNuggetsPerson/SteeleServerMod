package hsteele.steeleservermod.Commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.Steeleservermod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;

public class RunCommand {

    public static String RunFolder = "scripts";

    public static void register(CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher, boolean dedicated) {

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

        LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource> runCommand = CommandManager.literal("run")
                .requires(source -> source.hasPermissionLevel(4));

        String[] scripts = {
                "backup.sh",
                "updateMods.sh"
        };

        for (String script : scripts) {
            runCommand.then(CommandManager.literal(script)
                    .executes(ctx -> {
                        return run(ctx, script);
                    }));
        }

        dispatcher.register(runCommand);
    }

    private static int run(CommandContext<ServerCommandSource> context, String scriptName) throws CommandSyntaxException {

        ServerCommandSource source = context.getSource();
        File scriptFile = new File(RunFolder, scriptName);

        if (!scriptFile.exists() || !scriptFile.isFile()) {
            source.sendError(Text.literal("Script not found: " + scriptFile.getPath()));
            return 0;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", scriptFile.getAbsolutePath());

//            builder.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")));
//            builder.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null")));

            Process process = builder.start();
            source.sendFeedback(() -> Text.literal("Running script: " + scriptName), true);
            int exitCode = process.waitFor();

            source.sendFeedback(() -> Text.literal(scriptName + " completed."), true);
            return exitCode;

        } catch (IOException | InterruptedException e) {
            source.sendError(Text.literal("Failed to execute script: " + e.getMessage()));
            return -1;
        }
    }
}
