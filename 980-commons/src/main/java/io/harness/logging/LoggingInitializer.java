package io.harness.logging;

import io.harness.eraro.MessageManager;
import io.harness.exception.WingsException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@UtilityClass
@Slf4j
public class LoggingInitializer {
  public static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static boolean initialized;

  /**
   * Initialize logging.
   */
  public static void initializeLogging() {
    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    RollingFileAppender<?> appender = (RollingFileAppender<?>) logger.getAppender("file");
    SizeAndTimeBasedRollingPolicy<?> policy = (SizeAndTimeBasedRollingPolicy<?>) appender.getRollingPolicy();
    TimeBasedFileNamingAndTriggeringPolicy<?> trigger = policy.getTimeBasedFileNamingAndTriggeringPolicy();
    trigger.setCurrentTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    log.info("reset log files");
    trigger.setCurrentTime(0);

    log.info("start initializeLogging");
    if (!initialized) {
      log.info("reset values initializeLogging");
      try (InputStream in = LoggingInitializer.class.getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
        MessageManager.getInstance().addMessages(in);
      } catch (IOException exception) {
        throw new WingsException(exception);
      }

      initialized = true;
    }
  }
}
