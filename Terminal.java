import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.nio.file.*;
import java.nio.file.Files;
import java.io.*;
import java.io.FileReader;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Terminal {
    private class PrintManager{
        private String buffer = "";
        private int NEW_LINE_LIMIT = 8;
        private int newLineCount = 0;
        private PrintStream outputStream = System.out;
        public void setPrintStream(PrintStream p){
            if(outputStream != System.out){
                outputStream.close();
            }
            outputStream = p;
        }
        public void print(String s){
            if(outputStream == System.out) {
                buffer += s;
                if (newLineCount < NEW_LINE_LIMIT)
                    print();
            }
            else{
                outputStream.print(s);
            }
        }
        public  void print(){
            int i;
            for(i = 0;i < buffer.length() && newLineCount < NEW_LINE_LIMIT; i++){
                if(buffer.charAt(i) == '\n')
                    newLineCount++;
                outputStream.print(buffer.charAt(i));
            }
            buffer = buffer.substring(i);
            if (newLineCount == NEW_LINE_LIMIT)
                outputStream.print("...");
        }
        public  void println(String s){
            print(s + System.getProperty("line.separator"));
        }
        public void more(){
            newLineCount = 0;
            print();
        }
        public boolean isEmpty(){
            return buffer.isEmpty();
        }
        public void end(){
            newLineCount = 0;
            buffer = "";
        }
        //clears all previous commands in console
        public void clear(){
            for(int i = 0;i < 100;i++){
                outputStream.println();
            }
        }
    }
    private File workingDirectory; //stores absolute path of current directory
    private PrintManager printManager = new PrintManager();
    private Parser parser;


    //cds into default working directory
    public Terminal(){
        workingDirectory = new File(System.getProperty("user.home"));

        parser = new Parser();
        try {
            parser.initializeCommands("C:\\Users\\Lina\\IdeaProjects\\OS-Assignment I\\src\\command_list.txt");
        }
        catch(IOException e){
            printManager.println("IO error reading command list: " + e);
        }
    }
    public static void main(String args[]) {
        Terminal mainTerminal = new Terminal();
        mainTerminal.prompt();
    }
    public void prompt(){
        Scanner scan = new Scanner(System.in);
        while (true){
            printManager.setPrintStream(System.out);
            if(printManager.isEmpty())
                printManager.print(workingDirectory.getAbsolutePath() + ">");
            try {
                processCommand(scan.nextLine());
            }
            catch(Exception e){
                printManager.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }

    public void processCommand(String input) throws Exception{
        if(input.trim().isEmpty()){
            printManager.end();
            return;
        }
        String[] commands = input.trim().split("\\|");
        for (int i = 0; i < commands.length; i++){
            printManager.setPrintStream(System.out);
            String command = commands[i].trim();
            if (command.charAt(0) == '<'){ //Handle reading commands from a file
                String filePath = command.substring(1).trim();
                File file = makeAbsolute(filePath);
                BufferedReader br = new BufferedReader(new FileReader(file));
                command = br.readLine();
                while (command != null){
                    processCommand(command);
                    command = br.readLine();
                }
                br.close();
            }
            else if (command.contains(">>")){
                String filePath = command.substring(command.indexOf(">>") + 2).trim();
                command = command.substring(0, command.indexOf(">>")).trim();
                File file = makeAbsolute(filePath);
                printManager.setPrintStream(new PrintStream(new FileOutputStream(file, true)));
            }
            else if (command.contains(">")){
                String filePath = command.substring(command.indexOf(">") + 1).trim();
                File file = makeAbsolute(filePath);
                command = command.substring(0, command.indexOf(">")).trim();
                printManager.setPrintStream(new PrintStream(new FileOutputStream(file, false)));
            }

            if (!parser.parse(command)){
                throw new Exception("Error: Command not recognized.");
            }
            else{
                if(!parser.getCmd().equals("more"))
                    printManager.end();
                switch(parser.getCmd()){
                    case "?":
                        args(parser.getArgs()[0]);
                        break;
                    case "cp":
                        cp(parser.getArgs()[0], parser.getArgs()[1]);
                        break;
                    case "cd":
                        if (parser.getArgsCount() == 0)
                            cd();
                        else cd(parser.getArgs()[0]);
                        break;
                    case "clear":
                        printManager.clear();
                        break;
                    case "ls":
                        if (parser.getArgsCount() == 0)
                            ls();
                        else ls(parser.getArgs()[0]);
                        break;
                    case "pwd":
                        pwd();
                        break;
                    case "mv":
                        mv(parser.getArgs()[0], parser.getArgs()[1]);
                        break;
                    case "rm":
                        rm(parser.getArgs()[0]);
                        break;
                    case "mkdir":
                        mkdir(parser.getArgs()[0]);
                        break;
                    case "rmdir":
                        rmdir(parser.getArgs()[0]);
                        break;
                    case "date":
                        date();
                        break;
                    case "cat":
                        if (parser.getArgsCount() == 1)
                            cat(parser.getArgs()[0]);
                        else cat(parser.getArgs()[0], parser.getArgs()[1]);
                        break;
                    case "more":
                        printManager.more();
                        break;
                    case "args":
                        args(parser.getArgs()[0]);
                        break;
                    case "help":
                        help();
                        break;
                    case "exit":
                        exit();
                        break;
                }
            }
        }
    }
    //copies files ONLY from a path to another
    public void cp(String sourcePath, String destinationPath )throws IOException,NoSuchFileException{
        File src = makeAbsolute(sourcePath);
        if(!src.exists())
            throw new NoSuchFileException(src.getAbsolutePath(),null,"does not exist");
        File dst = makeAbsolute(destinationPath);
        if(!dst.exists()){
             if(dst.isDirectory())
                throw new NoSuchFileException(dst.getAbsolutePath(),null,"does not exist");
        }
        else
            Files.copy(src.toPath(),dst.toPath().resolve(src.toPath().getFileName()),StandardCopyOption.REPLACE_EXISTING);
    }

    //moves/renames a file/directory
    public void mv(String sourcePath, String destinationPath)throws IOException,NoSuchFileException{
        File src = makeAbsolute(sourcePath);
        File dst = makeAbsolute(destinationPath);
        if(!src.exists()) {
            throw new NoSuchFileException(src.getAbsolutePath(), null, "does not exist.");
        }
        if(dst.isFile()){
            throw new IOException("Can't move into file.");
        }

        if(!dst.exists()){ //renaming
            Files.move(src.toPath(),src.toPath().resolveSibling(dst.getName()));
        }
        else
            Files.move(src.toPath(),dst.toPath().resolve(src.toPath().getFileName()),StandardCopyOption.REPLACE_EXISTING);
    }

    //deletes file given specific path
    public void rm(String sourcePath) throws IOException,NoSuchFileException{
        File f = makeAbsolute(sourcePath);
        if(!f.exists())
            throw new NoSuchFileException(sourcePath,null,"does not exist.");
        else if(f.isDirectory())
            throw new IOException("Cannot delete directory.");
        else if (!f.delete())
            throw  new IOException("Cannot delete file.");
    }
    //prints working directory
    public void pwd(){
        printManager.println(workingDirectory.getAbsolutePath());
    }





    //changes the current directory to the given one
    public void cd(String sourcePath)throws NoSuchFileException,IOException{
        if(sourcePath.equals("..")){
            String parent = workingDirectory.getParent();
            File f = new File(parent);
            workingDirectory = f.getAbsoluteFile();
        }
        else{
            File f = makeAbsolute(sourcePath);
            if(!f.exists()){
                throw new NoSuchFileException(f.getAbsolutePath(),null,"does not exist");
            }
            if(f.isFile()){
                throw new IOException("Can't cd into file");
            }
            else workingDirectory = f.getAbsoluteFile();
        }
    }

    //changes into default directory
    public void cd(){

        workingDirectory = new File(System.getProperty("user.home"));
    }

    //given a relative path changed into absolute, if directory exists
    public File makeAbsolute(String sourcePath){
        File f = new File(sourcePath);
        if(!f.isAbsolute()) {
            f = new File(workingDirectory.getAbsolutePath(), sourcePath);
        }
        return f.getAbsoluteFile();
    }

    //lists all files and directory in given directory
    public void ls(String sourcePath)throws NoSuchFileException{
        File f = makeAbsolute(sourcePath);
        if(!f.exists()){
            throw new NoSuchFileException(f.getAbsolutePath(),null, "does not exist.");
        }
        String[] arr =  f.list();
        int n = arr.length;
        for(int i = 0;i < n;i++){
            printManager.println(arr[i]);
        }
    }

    //lists all files in current working directory
    public void ls(){
        String[] arr =  workingDirectory.list();
        int n = arr.length;
        for(int i = 0;i < n;i++){
            printManager.println(arr[i]);
        }
    }

    //creates a new directory with the given name in a given directory
    public void mkdir(String newDir)throws NoSuchFileException,IOException{
        File f = makeAbsolute(newDir);
        if(!f.getParentFile().exists())
            throw new NoSuchFileException(newDir,null,"does not exist.");
        if(f.exists())
            throw new IOException("Directory already exists.");
        boolean created = f.mkdir();
        if(!created)
            throw new IOException("Cannot create directory.");
    }

    //deletes empty directory
    public void rmdir(String sourcePath)throws DirectoryNotEmptyException,NoSuchFileException,IOException{
        File f = makeAbsolute(sourcePath);
        if(!f.exists())
            throw new NoSuchFileException(f.getAbsolutePath(),null,"does not exist");
        if(f.isFile())
            throw new IOException("Cannot delete file");
        else if(!f.delete())
            throw new DirectoryNotEmptyException("Cannot delete non-empty directory.");
    }

    //concatenates files(?) and displays their content
    public void cat(String f1) throws NoSuchFileException,IOException {
        File file = makeAbsolute(f1);
        if(file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file.getAbsolutePath()));
            String line;
            while ((line = in.readLine()) != null) {
                printManager.println(line);
            }
            in.close();
        }
        else
            throw new NoSuchFileException(file.getAbsolutePath(),null,"does not exist");
    }

    public void cat(String source,String dest)throws IOException
    {
        FileInputStream instream = null;
        FileOutputStream outstream = null;

        File infile = makeAbsolute(source);
        File outfile = makeAbsolute(dest);
        if(!infile.exists() || !outfile.exists())
            throw new IOException("No such file exists.");
        instream = new FileInputStream(infile);
        outstream = new FileOutputStream(outfile,true);

        byte[] buffer = new byte[1024];

        int length;
        while ((length = instream.read(buffer)) > 0)
        {
            outstream.write(buffer, 0, length);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(outfile)))
        {
            String line;
            while ((line = br.readLine()) != null) {
                printManager.println(line);
            }
        }
        instream.close();
        outstream.close();
    }



    /* prints out
        1. number of arguments
        2. all parameters
    given to a specific command */
    public void args(String command) {
        Parser.Command c = parser.getCommand(command);
        printManager.println(c.getCommand() + " " + c.getParameterCount() + " parameter/s.");
        printManager.println("Help: " + c.getHelp());
    }
    //prints system date
    public void date(){
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        LocalDateTime now = LocalDateTime.now();
        printManager.println(dateTime.format(now));
    }

    /*lists all program functionalities including
        1. all commands and their parameters
        2. the cmd for date
        3. the exit cmd*/
    public void help(){
        ArrayList <Parser.Command> commands = parser.getCommands();
        for(int i = 0;i < commands.size();i++){
            printManager.println(commands.get(i).getCommand() + " "+ commands.get(i).getParameterCount());
            printManager.println(commands.get(i).getHelp());
        }
        printManager.println("Current date/time: " + parser.getCommand("date"));
        printManager.println(parser.getCommand("date").getHelp());
        printManager.println("Exit: " + parser.getCommand("exit"));
        printManager.println(parser.getCommand("exit").getHelp());
//
    }

    //terminates program
    public void exit(){System.exit(0);}



}
