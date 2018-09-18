package example.armeria.server.degradation;

import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.decorator.LoggingDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;

@LoggingDecorator(
        requestLogLevel = LogLevel.INFO,            // Log every request sent to this service at INFO level.
        successfulResponseLogLevel = LogLevel.INFO  // Log every response sent from this service at INFO level.
)
public class CacheableService {
    private static final Logger logger = LoggerFactory.getLogger(CacheableService.class);

    public static Random rand = new Random();

    public CacheableService() {
    }

    /**
     * Each backend service may fail randomly.
     */
    @Get("/random")
    public String randomResult() {
        if (rand.nextBoolean()) {
            return "OK:" + new Date();
        } else {
            throw new RuntimeException("Randomly failed.");
        }
    }
}
