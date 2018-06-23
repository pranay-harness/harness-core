package software.wings.common.cache;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

/**
 * The Class ResponseCodeCache.
 */
public class ResponseCodeCache {
  @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
  protected static Logger logger = LoggerFactory.getLogger(ResponseCodeCache.class);

  private static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static final ResponseCodeCache instance = new ResponseCodeCache();

  private final Properties messages;

  private ResponseCodeCache() {
    messages = new Properties();
    try (InputStream in = getClass().getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
      messages.load(in);
    } catch (IOException exception) {
      throw new WingsException(exception);
    }
  }

  /**
   * Gets instance.
   *
   * @return singleton instance for the cache.
   */
  public static ResponseCodeCache getInstance() {
    return instance;
  }

  public ResponseMessage rebuildMessage(ResponseMessage inputMessage, Map<String, Object> params) {
    if (isNotEmpty(inputMessage.getMessage())) {
      return inputMessage;
    }

    return aResponseMessage()
        .code(inputMessage.getCode())
        .message(prepareMessage(inputMessage.getCode(), params))
        .level(inputMessage.getLevel())
        .build();
  }

  public String prepareMessage(ErrorCode errorCode, Map<String, Object> params) {
    String message = messages.getProperty(errorCode.name());
    if (message == null) {
      logger.error("Response message for error code {} is not provided!", errorCode.name());
      message = errorCode.name();
    }
    return prepareMessage(message, params);
  }

  private String prepareMessage(String message, Map<String, Object> params) {
    message = StrSubstitutor.replace(message, params);
    if (message.matches(".*(\\$\\$)*\\$\\{.*")) {
      logger.error(MessageFormat.format(
          "Insufficient parameter from [{0}] in message \"{1}\"", String.join(", ", params.keySet()), message));
    }
    return message;
  }
}
