package org.lordofthejars.villains.crimes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;

import java.util.stream.Collectors;

public class CrimesVerticle extends AbstractVerticle {

    private JDBCClient jdbcClient;

    public static void main(String args[]) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(CrimesVerticle.class.getName());
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

        jdbcClient.rxGetConnection()
            .flatMap(connection -> connection
                .rxExecute("create table crime(id int primary key, name varchar, villain varchar, wiki varchar)")
                .flatMap(v ->
                    connection.rxExecute("insert into crime values(1, 'Moon', 'Gru', 'https://en.wikipedia" +
                        ".org/wiki/Moon'), "
                        + "(2, 'Times Square JumboTron', 'Gru', 'https://en.wikipedia.org/wiki/One_Times_Square'), "
                        + "(3, 'Kryptonite', 'Lex Luthor', 'https://en.wikipedia.org/wiki/Kryptonite')"
                    )
                )
                .doAfterTerminate(connection::close))
            .flatMap(v -> {
                Router router = Router.router(vertx);
                router.route().handler(BodyHandler.create());
                router.get("/crimes/:villain").handler(this::handleGetCrimes);
                router.get("/health").handler(this::handleHealtchCheck);
                router.get("/version").handler(this::handleGetVersion);
                return vertx.createHttpServer().requestHandler(router::accept).rxListen(8080);
            })
            .toCompletable()
            .subscribe(RxHelper.toSubscriber(fut));
    }

    private void handleGetVersion(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        vertx.fileSystem()
            .rxReadFile("build-info.json")
            .map(Buffer::toJsonObject)
            .subscribe(json -> response.end(json.getString("version")), routingContext::fail);
    }

    private void handleHealtchCheck(RoutingContext routingContext){
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(200).end();
    }

    private void handleGetCrimes(RoutingContext routingContext) {
        String villainName = routingContext.request().getParam("villain");
        HttpServerResponse response = routingContext.response();
        if (villainName == null) {
            sendError(400, response);
        } else {
            jdbcClient.rxGetConnection()
                .flatMap(connection ->
                    connection.rxQueryWithParams("SELECT name, villain, wiki FROM crime WHERE villain=?", new JsonArray().add(villainName))
                        .map(resultSet -> resultSet.getRows().stream().map(Crime::new).collect(Collectors.toList()))
                        .doAfterTerminate(connection::close))
                .subscribe(crimes -> {
                        if (crimes.size() == 0) {
                            sendError(404, response);
                        } else {
                            response.putHeader("content-type", "application/json").end(Json.encodePrettily(crimes));
                        }
                    },
                    routingContext::fail);
        }
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }
}
