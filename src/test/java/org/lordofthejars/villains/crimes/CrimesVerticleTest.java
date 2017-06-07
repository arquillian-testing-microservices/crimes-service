package org.lordofthejars.villains.crimes;

import io.vertx.core.Vertx;
import java.util.concurrent.CountDownLatch;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;

public class CrimesVerticleTest {

    private static Vertx vertx;

    @BeforeClass
    public static void deployVerticle() throws InterruptedException {
        final CountDownLatch waitVerticleDeployed = new CountDownLatch(1);

        new Thread(() -> {
            vertx = Vertx.vertx();
            vertx.deployVerticle(CrimesVerticle.class.getName(), event -> {
                if (event.failed()) {
                    throw new IllegalStateException("Cannot deploy Crimes Verticle");
                }
                waitVerticleDeployed.countDown();
            });
        }).start();
        waitVerticleDeployed.await();
    }

    @Test
    public void should_get_crimes_from_villains() {
        given()
            .when()
            .get("crimes/{villain}", "Gru")
            .then()
            .assertThat()
            .body("name", CoreMatchers.hasItems("Moon", "Times Square JumboTron"))
            .body("wiki", CoreMatchers.hasItems("https://en.wikipedia.org/wiki/Moon", "https://en.wikipedia.org/wiki/One_Times_Square"));
    }
}
