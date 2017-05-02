package org.lordofthejars.villains.crimes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.List;
import java.util.stream.Collectors;
import org.lordofthejars.villains.crimes.util.Runner;

public class CrimesVerticle extends AbstractVerticle {

    private JDBCClient jdbcClient;

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Runner.runExample(CrimesVerticle.class);
    }

    @Override
    public void start(Future<Void> fut) {

        jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
            .put("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
            .put("driver_class", "org.h2.Driver")
            .put("user", "sa")
            .put("password", "")
            .put("max_pool_size", 30));

        Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        startBackend(
            (connection) -> createSomeData(connection,
                (nothing) -> startWebApp(
                    (http) -> completeStartup(http, fut)
                ), fut
            ), fut);
    }

    private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                fut.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    private void createSomeData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            final SQLConnection connection = result.result();
            connection.execute("create table crime(id int primary key, name varchar, villain varchar, wiki varchar)", res -> {

                if (res.failed()) {
                    fut.fail(res.cause());
                    connection.close();
                }

                // insert some test data
                connection.execute("insert into crime values(1, 'Moon', 'Gru', 'https://en.wikipedia.org/wiki/Moon'), "
                        + "(2, 'Times Square JumboTron', 'Gru', 'https://en.wikipedia.org/wiki/One_Times_Square'), "
                        + "(3, 'Kryptonite', 'Lex Luthor', 'https://en.wikipedia.org/wiki/Kryptonite')",

                    insert -> connection.close(done -> {
                        if (done.failed()) {
                            fut.fail(res.cause());
                            connection.close();
                        } else {
                            next.handle(Future.succeededFuture());
                            connection.close();
                        }
                    }));
            });
        }
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {

        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/crimes/:villain").handler(this::handleGetCrimes);

        vertx.createHttpServer().requestHandler(router::accept).listen(8080, next::handle);

    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }

    private void handleGetCrimes(RoutingContext routingContext) {
        String villainName = routingContext.request().getParam("villain");
        HttpServerResponse response = routingContext.response();
        if (villainName == null) {
            sendError(400, response);
        } else {

            jdbcClient.getConnection( ar -> {
                SQLConnection connection = ar.result();
                connection.query("SELECT name, villain, wiki FROM crime", result -> {
                    List<Crime> crimes = result.result().getRows().stream()
                                            .map(Crime::new)
                                            .collect(Collectors.toList());
                    if (crimes.size() == 0) {
                        sendError(404, response);
                    } else {
                        response.putHeader("content-type", "application/json").end(Json.encodePrettily(crimes));
                    }
                });
            });
        }
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }
}
