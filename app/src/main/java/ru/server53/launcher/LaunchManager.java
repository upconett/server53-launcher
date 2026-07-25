package ru.server53.launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.LaunchContext;


public class LaunchManager {
    private final ClientJson clientJson;
    private final LaunchContext context;


    public LaunchManager(
        ClientJson clientJson,
        LaunchContext context
    ) throws IOException
    {
        this.clientJson = clientJson;
        this.context = context;
    }

    
    public void launchGame() throws IOException 
    {
        List<String> allArgs = new ArrayList<>();
        allArgs.add("java");
        for (var arg : clientJson.argumentsJVM()) {
            if (arg.isAllowed(context)) {
                allArgs.addAll(List.of(arg.getPopulatedValues(context)));
            }
        }
        allArgs.add(clientJson.mainClass());
        for (var arg : clientJson.argumentsGame()) {
            if (arg.isAllowed(context)) {
                allArgs.addAll(List.of(arg.getPopulatedValues(context)));
            }
        }
        String[] command = allArgs.toArray(String[]::new);

        System.out.println("Using command: ");
        for (var arg : command) { System.out.print("\""+arg+"\" "); }
        System.out.println();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.start();
    }
}
