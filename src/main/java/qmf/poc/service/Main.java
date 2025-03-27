package qmf.poc.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;

public class Main {
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        Router router = Router.router(vertx);
        router.get("/some/path").handler(ctx -> {
            Future<ServerWebSocket> fut = ctx.request().toWebSocket();
            fut.onSuccess(ws -> {
                System.out.println("Connection created");
                // ws.accept();
                ws.frameHandler(frame -> {
                    // Echo the message back to the client
                    System.out.println("thread: " + Thread.currentThread().getName());
                    // Offload work to a worker thread using blockingContext
                    /*
                    ctx.vertx().executeBlocking(v -> {
                        // Simulate some heavy processing
                        System.out.println("Thread (inside blockingContext): " + Thread.currentThread().getName());
                        // Simulate work with frame (e.g., CPU intensive)
                        String result = "Processed frame: " + frame.textData();

                        // Complete the blocking operation and send response
                        ws.writeTextMessage("Echo frame: " + result);
                    });
                     */
                    ctx.vertx().executeBlocking(() -> {
                        // Simulate some heavy processing
                        System.out.println("Thread (inside blockingContext): " + Thread.currentThread().getName());
                        // Simulate work with frame (e.g., CPU intensive)
                        String result = "Processed frame: " + frame.textData();
                        return result; // Return the result after the blocking task is done
                    }, res -> {
                        if (res.succeeded()) {
                            // Send the result back to the WebSocket client
                            ws.writeTextMessage("Echo frame: " + res.result());
                        } else {
                            // Handle failure
                            System.err.println("Blocking operation failed: " + res.cause());
                        }
                    });
                });

                // Handle WebSocket close
                ws.closeHandler(v -> {
                    System.out.println("Connection closed");
                });

                // Handle WebSocket errors
                ws.exceptionHandler(t -> {
                    t.printStackTrace();
                    ws.close();
                });
            });
        });

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(8080);
    }
}
