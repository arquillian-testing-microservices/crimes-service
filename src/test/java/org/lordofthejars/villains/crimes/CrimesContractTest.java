package org.lordofthejars.villains.crimes;

import io.vertx.core.Vertx;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import org.arquillian.algeron.pact.provider.spi.Provider;
import org.arquillian.algeron.pact.provider.spi.Target;
import org.arquillian.algeron.provider.core.retriever.ContractsFolder;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.arquillian.algeron.pact.provider.assertj.PactProviderAssertions.assertThat;

@RunWith(Arquillian.class)
@Provider("crimes")
@ContractsFolder("~/crimescontract")
public class CrimesContractTest {

    @ArquillianResource
    Target target;

    @Test
    public void should_validate_contract() {
        assertThat(target).withUrl(getCrimesServer()).satisfiesContract();
    }

    private static Vertx vertx;


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
