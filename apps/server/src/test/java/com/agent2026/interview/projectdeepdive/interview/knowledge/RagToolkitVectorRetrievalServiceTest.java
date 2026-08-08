package com.agent2026.interview.projectdeepdive.interview.knowledge;

import com.agent2026.interview.config.RagToolkitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RagToolkitVectorRetrievalServiceTest {

    private HttpServer server;
    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void successfulSearchOnlyExposesNonBlankHitContent() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            token.set(exchange.getRequestHeaders().getFirst("X-Rag-Token"));
            respond(exchange, 200, """
                    {"profile":"interview","hits":[
                      {"content":"  first snippet  ","score":0.91},
                      {"content":""},
                      {"content":"second snippet","metadata":{"private":"value"}}
                    ]}
                    """);
        });

        RagToolkitProperties properties = properties();
        properties.setSearchToken("test-token");
        RetrievalContext result = service(properties).retrieve("线程池如何避免资源耗尽");

        assertThat(result.snippets()).containsExactly("first snippet", "second snippet");
        assertThat(result.degraded()).isFalse();
        assertThat(requestBody).hasValueSatisfying(body -> {
            assertThat(body).contains("线程池如何避免资源耗尽");
            assertThat(body).contains("\"top_k\":5");
            assertThat(body).contains("\"filters\":{}");
        });
        assertThat(token).hasValue("test-token");
    }

    @Test
    void disabledModeDoesNotCallRemoteService() {
        RagToolkitProperties properties = new RagToolkitProperties();
        properties.setEnabled(false);
        properties.setBaseUrl("http://127.0.0.1:1");

        RetrievalContext result = service(properties).retrieve("query");

        assertThat(result.snippets()).isEmpty();
        assertThat(result.degraded()).isFalse();
    }

    @Test
    void timeoutReturnsDegradedContext() throws IOException {
        startServer(exchange -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        RagToolkitProperties properties = properties();
        properties.setReadTimeout(Duration.ofMillis(50));

        RetrievalContext result = service(properties).retrieve("query");

        assertThat(result.snippets()).isEmpty();
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void nonSuccessfulResponseReturnsDegradedContext() throws IOException {
        startServer(exchange -> respond(exchange, 503, "service unavailable"));

        RetrievalContext result = service(properties()).retrieve("query");

        assertThat(result.snippets()).isEmpty();
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void invalidResponseReturnsDegradedContext() throws IOException {
        startServer(exchange -> respond(exchange, 200, "{\"hits\":\"not-an-array\"}"));

        RetrievalContext result = service(properties()).retrieve("query");

        assertThat(result.snippets()).isEmpty();
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void emptyHitListIsAHealthyNoResult() throws IOException {
        startServer(exchange -> respond(exchange, 200, "{\"hits\":[]}"));

        RetrievalContext result = service(properties()).retrieve("query");

        assertThat(result.snippets()).isEmpty();
        assertThat(result.degraded()).isFalse();
    }

    private RagToolkitProperties properties() {
        RagToolkitProperties properties = new RagToolkitProperties();
        properties.setEnabled(true);
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        properties.setTopK(5);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return properties;
    }

    private RagToolkitVectorRetrievalService service(RagToolkitProperties properties) {
        return new RagToolkitVectorRetrievalService(properties, new ObjectMapper());
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", handler);
        executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
