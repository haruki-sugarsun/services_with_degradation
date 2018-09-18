package example.armeria.server.degradation;

import com.linecorp.armeria.client.Client;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpMessage;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.decorator.LoggingDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static example.armeria.server.degradation.ExampleMain.CACHEABLE_SERVICE_PORT;
import static example.armeria.server.degradation.ExampleMain.IGNORABLE_SERVICE_PORT;
import static example.armeria.server.degradation.ExampleMain.IMPORTANT_SERVICE_PORT;

@LoggingDecorator(
        requestLogLevel = LogLevel.INFO,            // Log every request sent to this service at INFO level.
        successfulResponseLogLevel = LogLevel.INFO  // Log every response sent from this service at INFO level.
)
public class MainService {
    private static final Logger logger = LoggerFactory.getLogger(MainService.class);


    private final HttpClient importantClient;
    private final HttpClient ignorableClient;
    private final HttpClient cacheableClient;

    //
    // Note that we need to consider multi-threading and per-user data in actual service.
    private Optional<String> cachedDataForCacheableBackend = Optional.empty();

    public MainService() {
        importantClient = HttpClient.of("http://127.0.0.1:" + IMPORTANT_SERVICE_PORT);
        ignorableClient = HttpClient.of("http://127.0.0.1:" + IGNORABLE_SERVICE_PORT);
        cacheableClient = HttpClient.of("http://127.0.0.1:" + CACHEABLE_SERVICE_PORT);
    }

    /**
     * Each backend service may fail randomly.
     */
    @Get("/random")
    public String randomResult() {
        // Execute backend requests in parallel.
        HttpResponse importantResponse = importantClient.get("/random");
        HttpResponse ignorableResponse = ignorableClient.get("/random");
        HttpResponse cacheableResponse = cacheableClient.get("/random");

        BackendResult importantResult = fromHttpResponse(importantResponse);
        if (importantResult.isSuccess()) {
            // Service is happy. We have at least something to show.
        } else {
            // Service should show an error. and we do not care about other backends.
            logger.error("An important backend has error. {}", importantResult.getValue());
            return "Sorry. We have a service issue. Nothing to show.";
        }

        BackendResult ignorableResult = fromHttpResponse(ignorableResponse);
        BackendResult cacheableResult = fromHttpResponse(cacheableResponse);
        if (cacheableResult.isSuccess()) {
            // Update the cache if the cacheable backend returns data.
            cachedDataForCacheableBackend = cacheableResult.getValue();
        }

        return makeSummary("Random results", importantResult, ignorableResult, cacheableResult);
    }

    private String makeSummary(String headline, BackendResult importantResult, BackendResult ignorableResult, BackendResult cacheableResult) {
        StringBuilder sb = new StringBuilder();

        sb.append("*" + headline + "*\n\n");

        // Essential part.
        sb.append("Important Backend returns " + importantResult.getValue().get() + "\n");

        // Optional part.
        if (ignorableResult.isSuccess()) {
            sb.append("Ignorable Backend returns " + ignorableResult.getValue().get() + "\n");
        } else {
            sb.append("Ignorable Backend returns nothing. But we can continue.\n");
        }

        // Cacheable part.
        if (cacheableResult.isSuccess()) {
            sb.append("Cacheable Backend returns " + cacheableResult.getValue().get() + "\n");
        } else if (cachedDataForCacheableBackend.isPresent()) {
            sb.append("Cacheable Backend returns nothing. But we can show old data, which is " + cachedDataForCacheableBackend.get() + ".\n");
        } else {
            sb.append("Cacheable Backend returns nothing. And we do not have cached one.\n");
        }

        sb.append("\n\n(important:" + (importantResult.isSuccess() ? "OK" : "failed")
                + ", ignorable:" + (ignorableResult.isSuccess() ? "OK" : "failed")
                + ", cacheable:" + (cacheableResult.isSuccess() ? "OK" : "failed") + ")");
        return sb.toString();
    }


    public BackendResult fromHttpResponse(HttpResponse response) {
        try {
            AggregatedHttpMessage result = response.aggregate().join();

            int status = result.status().code();
            if (200 <= status && status < 300) {
                return new BackendResult(true, Optional.of(result.content().toStringAscii()));
            } else {
                logger.info("Backend error with HTTP result. status={}.", status);
            }
        } catch (Exception e) {
            logger.error("Backend error with exception.", e);
        }

        // Error case.
        return new BackendResult(false, Optional.empty());
    }

}
