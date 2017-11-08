package org.lordofthejars.villains.crimes;

import io.vertx.core.Vertx;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import org.junit.BeforeClass;
import org.junit.Test;

//1. Runner
//2. Provider name
//3. Defines contract location
public class CrimesContractTest {


    private static Vertx vertx;

    //4. Set http client to replay


    @Test
    public void should_validate_contract() {
        //5. Pact Provider assertion
    }

    private URL getCrimesServer() {
        try {
            return new URL("http://localhost:8080");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

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

}
