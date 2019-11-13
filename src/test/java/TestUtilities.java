import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.util.ByteString;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static akka.util.ByteString.empty;

/*
    Utilities class for Akka Server Tests
 */
public class TestUtilities {

    private String parse(ByteString line) {
        return line.utf8String();
    }

    // Load ByteString into memory and use CompletableFuture pipeline to asynchronously transform bytes to String

    public String getHttpMessageBodyAsString(CompletionStage<HttpResponse> responseFuture) throws ExecutionException, InterruptedException {

        final ActorSystem system = ActorSystem.create();

        final Materializer materializer = ActorMaterializer.create(system);

        final CompletionStage<HttpEntity.Strict> strictEntity = responseFuture.toCompletableFuture().get().entity()
                .toStrict(FiniteDuration.create(3, TimeUnit.SECONDS).toMillis(), materializer);

        final CompletionStage<String> data =
                strictEntity
                        .thenCompose(strict ->
                                strict.getDataBytes()
                                        .runFold(empty(), (acc, b) -> acc.concat(b), materializer)
                                        .thenApply(this::parse)
                        );

        // TODO : Should probably replace this, could end up in infinite loop
        while (! ((CompletableFuture) data).isDone()){
            // Wait for process to finish
        }

        return ((CompletableFuture) data).getNow(null).toString()
                .replace("[" , "").replace("]", "");
    }

    public String postTicket() throws InterruptedException, ExecutionException {
        final ActorSystem system = ActorSystem.create();

        final CompletionStage<HttpResponse> postRequest1 =
                Http.get(system)
                        .singleRequest((HttpRequest.POST("http://localhost:8080/ticket/10")));

        String messageReturned = getHttpMessageBodyAsString(postRequest1);

        StringBuilder fileLocation = new StringBuilder(messageReturned.substring(messageReturned.indexOf(":") + 2));

        return fileLocation.toString();


    }
}
