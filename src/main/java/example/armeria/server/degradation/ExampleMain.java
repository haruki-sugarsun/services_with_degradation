package example.armeria.server.degradation;

import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ExampleMain {
    private static final Logger logger = LoggerFactory.getLogger(ExampleMain.class);

    public static final int MAIN_SERVICE_PORT = 8080;
    public static final int IMPORTANT_SERVICE_PORT = 8081;
    public static final int IGNORABLE_SERVICE_PORT = 8082;
    public static final int CACHEABLE_SERVICE_PORT = 8083;

    public static void main(String[] args) throws Exception {
        logger.info("Example for services with degradation.");

        // A server instance serving for the end users.
        Server mainHttpServer = mainHttpServer();

        // The service depends on these servers. One is important, but the others can be degraded in severe situation.
        Server importantBackendServer = importantBackendServer();
        Server ignorableBackendServer = ignorableBackendServer();
        Server cacheableBackendServer = cacheableBackendServer();

        // Start the servers.
        importantBackendServer.start();
        ignorableBackendServer.start();
        cacheableBackendServer.start();

        mainHttpServer.start();
    }

    private static Server mainHttpServer() {
        return new ServerBuilder().port(MAIN_SERVICE_PORT, SessionProtocol.HTTP)
                .annotatedService("/demo", new MainService())
                .build();
    }

    private static Server importantBackendServer() {
        return new ServerBuilder().port(IMPORTANT_SERVICE_PORT, SessionProtocol.HTTP)
                .annotatedService("/", new ImportantService())
                .build();
    }

    private static Server cacheableBackendServer() {
        return new ServerBuilder().port(IGNORABLE_SERVICE_PORT, SessionProtocol.HTTP)
                .annotatedService("/", new IgnorableService())
                .build();
    }

    private static Server ignorableBackendServer() {
        return new ServerBuilder().port(CACHEABLE_SERVICE_PORT, SessionProtocol.HTTP)
                .annotatedService("/", new CacheableService())
                .build();
    }
}
