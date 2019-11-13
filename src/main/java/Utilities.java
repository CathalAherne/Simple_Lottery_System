import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.directives.RouteAdapter;
import akka.http.javadsl.server.AllDirectives;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/*
    Utilities class for Akka Server
 */

public class Utilities extends AllDirectives{

    private Random random;
    private List<String> immutableTickets;
    public final static String FILE_ROOT = "ticket";


    public Utilities(){
        random = new Random();
        immutableTickets = new ArrayList<>();

    }

    public UUID createTicketWithNLines(int n){
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        File ticket = new File(FILE_ROOT + "/" + randomUUIDString);
        writeNLinesToFile(n, ticket, false);
        return uuid;

    }

    public void writeNLinesToFile(int n, File file, Boolean isAppend){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file, isAppend))){
            for (int lineNum = 0 ; lineNum < n ; lineNum ++) {
                StringBuilder line = new StringBuilder();
                for (int colNum = 0; colNum < 3; colNum++) {
                    line.append(random.nextInt(3));
                }
                writer.write(line.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendNLinesToTicket(String fileLocation, int n) throws IOException {
        File file = new File(FILE_ROOT + "/" + fileLocation);
        writeNLinesToFile(n, file, true);
    }


    public RouteAdapter getTicket(Path path){
        RouteAdapter complete;
        try {
            complete = complete(StatusCodes.ACCEPTED, Files.readAllLines(path).toString()
                .trim().replace(",", "\n")
                    .replace("[", " ")
                        .replace("]", ""));
        } catch (IOException e) {
            complete = complete(StatusCodes.BAD_REQUEST, "There is no file at this location");
        }
        return complete;
    }

    public RouteAdapter getAllTickets(){
        RouteAdapter complete = null;
        try {
            File[] rootFolder = new File(FILE_ROOT).listFiles();
            StringBuilder allFiles = new StringBuilder();
            for (File file : rootFolder) {
                Path path = Paths.get(file.getAbsolutePath());
                allFiles.append(Files.readAllLines(path).toString()
                        .trim().replace(",", "\n")
                            .replace("[", " ")
                                .replace("]", ""));
                allFiles.append("\n");
            }
            complete = complete(StatusCodes.ACCEPTED, allFiles.toString());
        } catch (IOException e) {
            complete = complete(StatusCodes.BAD_REQUEST, "There is no file at this location");
        }
        return complete;
    }

    public RouteAdapter getTicketStatus(String fileDirectory){
        RouteAdapter complete;
        StringBuilder result = new StringBuilder();
        try (BufferedReader bf = new BufferedReader(new FileReader(new File(FILE_ROOT + "/" + fileDirectory)))) {
            String line;
            while ((line = bf.readLine()) != null){
                String[] lotteryLines  = line.split("\n");
                for (String nums : lotteryLines){
                    result.append(getLineStatus(nums));
                    result.append("\n");
                }
            }
            complete = complete(StatusCodes.ACCEPTED, result.toString());
            immutableTickets.add(fileDirectory);
        }
        catch(IOException e){
            complete = complete(StatusCodes.BAD_REQUEST, "There is no file at this location");
        }
        return complete;
    }

    public static int getLineStatus(String lineNums){
        int[] values = {Character.getNumericValue(lineNums.charAt(0)),
                Character.getNumericValue(lineNums.charAt(1)),
                Character.getNumericValue(lineNums.charAt(2))};

        int status = 0;

        if (values[0] + values[1] + values[2] == 2){
            status = 10;
        }
        else if (values[0] == values[1] && values[1] == values[2]){
            status = 5;
        }
        else if (values[0] != values[1] && values[0] != values[2]){
            status = 1;
        }

        return status;
    }

    public List<String> getImmutableTickets(){
        return immutableTickets;
    }
}
