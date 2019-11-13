import akka.actor.ActorSystem;

import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import java.io.*;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestAkkaServer {

    private final ActorSystem system;
    private TestUtilities testUtils;

    public TestAkkaServer(){
        system = ActorSystem.create();
        testUtils = new TestUtilities();
    }

    @AfterClass
    public static void deleteFilesAfterTests() {
        File[] rootFolder = new File(Utilities.FILE_ROOT).listFiles();

        for (File file : rootFolder) {
            if (!file.getName().equals("Ticket-For-Status-Test")) {
                file.delete();
            }
        }
    }


    @Test
    public void testGetAllTickets() throws ExecutionException, InterruptedException, IOException
    {
        testUtils.postTicket();
        testUtils.postTicket();

        File[] rootFolder = new File(Utilities.FILE_ROOT).listFiles();
        StringBuilder allFiles = new StringBuilder();
        for (File file : rootFolder) {
            Path path = Paths.get(file.getAbsolutePath());
            allFiles.append(Files.readAllLines(path).toString()
                    .trim().replace(",", "\n")
                        .replace("[", " ")
                            .replace("]", ""));
            allFiles.append("\n");
        }

        final CompletionStage<HttpResponse> responseFuture =
                Http.get(system)
                        .singleRequest(HttpRequest.create("http://localhost:8080/ticket"));

        assertEquals(allFiles.toString().replace("[" , "").replace("]", ""), testUtils.getHttpMessageBodyAsString(responseFuture));

    }

    @Test
    public void testGetSpecificTicket() throws ExecutionException, InterruptedException, IOException
    {
        String fileLocation = testUtils.postTicket();

        final CompletionStage<HttpResponse> responseFuture =
                Http.get(system)
                        .singleRequest(HttpRequest.create("http://localhost:8080/" + fileLocation));

        Path path = Paths.get(fileLocation);

        String fileContents = Files.readAllLines(path)
                .toString().trim().replace(",", "\n")
                    .replace("[", " ")
                        .replace("]", "");

        assertEquals(fileContents, testUtils.getHttpMessageBodyAsString(responseFuture));
    }

    /*
        This test fails on the first run for some reason, Ran out of time for investigation
     */

    @Test
    public void testPutTicket() throws ExecutionException, InterruptedException
    {

        String fileLocation = testUtils.postTicket();

        Http.get(system).singleRequest(HttpRequest.PUT("http://localhost:8080/" + fileLocation + "/3"));

        final CompletionStage<HttpResponse> getResponseFuture =
                Http.get(system)
                        .singleRequest(HttpRequest.create("http://localhost:8080/" + fileLocation));

        assertEquals(39, testUtils.getHttpMessageBodyAsString(getResponseFuture)
                .replace("\n" , "").replace(" ", "").length());

    }

    @Test
    public void testTicketCannotBeAmendedAfterStatusGenerated() throws ExecutionException, InterruptedException
    {
        // Test Status Generated is Correct
        final CompletionStage<HttpResponse> getStatusResponseFuture =
                Http.get(system)
                        .singleRequest(HttpRequest.create("http://localhost:8080/Status/" + "Ticket-For-Status-Test"));

        String[] ticketStatuses = testUtils.getHttpMessageBodyAsString(getStatusResponseFuture).trim().split("\n");

        List expectedTicketStatuses = Arrays.asList(5, 0, 1, 0, 0, 10, 10, 0, 1, 10);

        List<Integer> ticketStatusesAsIntegers = Arrays.asList(ticketStatuses).stream()
                .map(status -> Integer.parseInt(status.trim())).collect(Collectors.toList());

        Assert.assertEquals(expectedTicketStatuses, ticketStatusesAsIntegers );

        // Test ticket can not be amended after status is generated
        final CompletionStage<HttpResponse> putResponseFuture =  Http.get(system).singleRequest(HttpRequest.PUT("http://localhost:8080/" + "ticket/Ticket-For-Status-Test" + "/3"));

        String responseMessage = testUtils.getHttpMessageBodyAsString(putResponseFuture);

        assertEquals( putResponseFuture.toCompletableFuture().get().status(), StatusCodes.BAD_REQUEST );
        assertEquals( responseMessage, "Ticket can not be amended"  );

    }

    @Test
    public void testGetResponseWithIncorrectTicketNumber() throws ExecutionException, InterruptedException
    {
        final CompletionStage<HttpResponse> responseFuture =
                Http.get(system)
                        .singleRequest(HttpRequest.create("http://localhost:8080/" + "ticket/1-1-1-1"));

        assertEquals(StatusCodes.BAD_REQUEST, responseFuture.toCompletableFuture().get().status());
        assertEquals("There is no file at this location", testUtils.getHttpMessageBodyAsString(responseFuture));

    }
}

