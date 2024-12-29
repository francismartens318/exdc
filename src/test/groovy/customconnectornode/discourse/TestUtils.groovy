package customconnectornode.discourse

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory

class TestUtils {

    static void setupSpec() {
        // Set root logger to ERROR level
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.setLevel(Level.ERROR)

        // Enable DEBUG level for customconnectornode package
        Logger appLogger = (Logger) LoggerFactory.getLogger("customconnectornode")
        appLogger.setLevel(Level.DEBUG)

        Dotenv dotenv = Dotenv.configure()
                .directory("./ccnode")
                .load()

        System.setProperty("TRACKER_URL", dotenv.get("TRACKER_URL"))
        System.setProperty("TRACKER_API_KEY", dotenv.get("TRACKER_API_KEY"))
        System.setProperty("TRACKER_USER", dotenv.get("TRACKER_USER"))
    }

}
