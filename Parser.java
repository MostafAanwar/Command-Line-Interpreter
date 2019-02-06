import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {
    public class Command{
        private String command;
        private String helpText;
        private int parameterCount;

        public Command(String cmd, int c){
            command = cmd;
            parameterCount = c;
            helpText = "";
        }
        public Command(){}
        public int getParameterCount(){
            return parameterCount;
        }

        public void addHelp(String text){
            helpText += (helpText.isEmpty() ? "" : "\n") + text;
        }

        public void clearHelp(){
            helpText = "";
        }

        public String getHelp(){
            return helpText;
        }

        public String getCommand(){
            return command;
        }

        public String toString(){
            return command;
        }
    }

    private String cmd; //command taken from parse method
    private String[] args; //filled by cmd arguments from parse method
    private boolean isValidParse; // is true when a command is parsed correctly
    private ArrayList<Command> commands;

    /*
        Checks if path is a valid file path under the Windows path convention
     */
    public boolean isFilePath(String path){
        return Pattern.matches("[a-zA-Z]:(/[a-zA-Z][^\\s/]*)*/?", path);
    }

    public Parser(){
        commands = new ArrayList<>(); // init ArrayList
    }

    public int getArgsCount(){
        return args == null ? 0 : args.length;
    }

    public void initializeCommands(String commandListPath) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(commandListPath));

        Pattern commentPattern = Pattern.compile("^(//).*"); // Regex that matches comments
        Pattern helpPattern = Pattern.compile("^(::).*"); // Regex that matches help text
        Pattern commandPattern = Pattern.compile("[a-zA-Z?]+\\s\\d"); // Regex that matches commands

        String line = br.readLine();
        while (line != null){ // will exit loop when eof is reached
            if (commentPattern.matcher(line).matches()){ } //ignore if comment
            else if (helpPattern.matcher(line).matches()){ // line is a help element
                if (!commands.isEmpty()){
                    commands.get(commands.size() - 1).addHelp(line.substring(2)); // add help text to last command
                }
            }
            else if (commandPattern.matcher(line).matches()){ // line is a valid command
                String[] lineElements = line.split("\\s");
                commands.add(new Command(lineElements[0], Integer.parseInt(lineElements[1])));
            }
            line = br.readLine();
        }

        br.close(); // close file
    }

    /*verifies input,
     once verified, extracts input and fills parameters accordingly */
    public boolean parse(String input) {
        cmd = null;
        args = null;
        isValidParse = false;
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(input);
        ArrayList<String> argsList = new ArrayList<>();
        if (!matcher.find()){
            return false;
        }
        cmd = matcher.group();
        while (matcher.find()){
            if (matcher.group(1) != null){
                argsList.add(matcher.group(1));
            }
            else{
                argsList.add(matcher.group(2));
            }
        }
        args = new String[argsList.size()];
        argsList.toArray(args);

       Command c = getCommand(cmd, args.length);
        if (c != null){
            isValidParse = true;
            return true;
        }
        cmd = null;
        args = null;
        return false;
    }
    public Command getCommand(String c , int p){
        for (int i = 0; i < commands.size(); i++)
            if (commands.get(i).getCommand().equals(c) && p == commands.get(i).getParameterCount()){
                return commands.get(i);
            } //TODO uhh do logic to return multiple commands with diff parameter count
        return null;
    }

    public Command getCommand(String c){
        for (int i = 0; i < commands.size(); i++)
            if (commands.get(i).getCommand().equals(c)){
                return commands.get(i);
            } //TODO uhh do logic to return multiple commands with diff parameter count
        return null;
    }

    public ArrayList<Command> getCommands() {
        return commands;
    }

    /*
            Getter for cmd. Returns null if no valid parse is made.
         */
    public String getCmd() {
        return isValidParse ? cmd : null;
    }

    /*
        Getter for args. Returns null if no valid parse is made.
     */
    public String[] getArgs() {
        return isValidParse ? args: null;
    }

    /*
        Getter for isValidParse.
     */
    public boolean isValidParse() {
        return isValidParse;
    }
}
