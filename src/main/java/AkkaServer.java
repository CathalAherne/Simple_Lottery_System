import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionStage;

/*
    Rest Server which hosts a simple lottery ticketing system with GET, POST and PUT.
 */
public class AkkaServer extends AllDirectives implements Runnable{

    private Utilities utils;
    private final static String PATH_PREFIX = "ticket";

    public AkkaServer(){
        utils = new Utilities();
    }


    public void run(){
        // boot up server using the route as defined below
        ActorSystem system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        AkkaServer app = new AkkaServer();

        //In order to access all directives we need an instance where the routes are define.

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow;
        try {
            routeFlow = app.createRoute().flow(system, materializer);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Server Failed to Start");
        }
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), materializer);

        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
    }

    private Route createRoute(){
        return concat(
                get(() ->
                        pathPrefix(PATH_PREFIX, () ->
                                path(Segment -> {
                                    Path path = Paths.get(Utilities.FILE_ROOT + "/" + Segment);
                                    return utils.getTicket(path);
                                }))),
                get(() ->
                        pathPrefix(PATH_PREFIX, () -> {
                            return utils.getAllTickets();
                        })),
                get(() ->
                        pathPrefix("Status", () ->
                            path(Segment -> {
                            return utils.getTicketStatus(Segment);
                            }))),
                post(() ->
                        pathPrefix(PATH_PREFIX , () ->
                                path(Segment -> {
                                    UUID uuid = utils.createTicketWithNLines(Integer.parseInt(Segment));
                                            return complete(StatusCodes.CREATED, "Ticket created and stored at: " + PATH_PREFIX + "/" + uuid.toString());
                                        }
                                )
                        )
                ),
                put(() ->
                pathPrefix(PATH_PREFIX , () ->
                        path(PathMatchers.segment().slash(PathMatchers.segment()), (String fileDirectory, String numLines) -> {
                            try {
                                if (utils.getImmutableTickets().contains(fileDirectory)){
                                    return complete(StatusCodes.BAD_REQUEST, "Ticket can not be amended");
                                }
                                utils.appendNLinesToTicket(fileDirectory, Integer.parseInt(numLines));
                                return complete(StatusCodes.OK, "Ticket amended");
                            } catch (Exception e) {
                                return complete(StatusCodes.BAD_REQUEST, "Something went wrong");
                            }
                        }
                ))));
    }



    public static void main(String[] args) {
        Thread t1 = new Thread(new AkkaServer());
        t1.start();
    }
}
