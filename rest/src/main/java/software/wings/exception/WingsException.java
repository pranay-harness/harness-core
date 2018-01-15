package software.wings.exception;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.ResponseMessage.Level.ERROR;
import static software.wings.beans.ResponseMessage.Level.INFO;
import static software.wings.beans.ResponseMessage.Level.WARN;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.Scenario.API_CALL;
import static software.wings.utils.Switch.unhandled;

import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.Acuteness;
import software.wings.beans.ResponseMessage.Level;
import software.wings.common.cache.ResponseCodeCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * The generic exception class for the Wings Application.
 *
 * @author Rishi
 */
@Getter
public class WingsException extends WingsApiException {
  protected static Logger logger = LoggerFactory.getLogger(WingsException.class);

  private static final long serialVersionUID = -3266129015976960503L;

  /**
   * The Response message list.
   */
  private List<ResponseMessage> responseMessageList = new ArrayList<>();

  private Map<String, Object> params = new HashMap<>();

  /**
   * Instantiates a new Wings exception.
   *
   * @param message the message
   */
  public WingsException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message);
  }

  /**
   * Instantiates a new Wings exception.
   *
   * @param message the message
   * @param cause   the cause
   */
  public WingsException(String message, Throwable cause) {
    this(ErrorCode.UNKNOWN_ERROR, message, cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param cause the cause
   */
  public WingsException(Throwable cause) {
    this(ErrorCode.UNKNOWN_ERROR, cause);
  }

  /**
   * Instantiates a new Wings exception.
   *
   * @param message the message
   */
  public WingsException(ErrorCode errorCode, String message) {
    this(errorCode, message, (Throwable) null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   */
  public WingsException(ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param cause     the cause
   */
  public WingsException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param message   the message
   * @param cause     the cause
   */
  public WingsException(ErrorCode errorCode, String message, Throwable cause) {
    super(message == null ? errorCode.getCode() : message, cause);
    responseMessageList.add(aResponseMessage().code(errorCode).message(message).build());
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param params    the params
   * @param errorCode the error code
   */
  public WingsException(Map<String, Object> params, ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
    this.params = params;
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param responseMessage     the message
   */
  public WingsException(@NotNull ResponseMessage responseMessage) {
    super(responseMessage.getMessage() == null ? responseMessage.getCode().name() : responseMessage.getMessage(), null);
    responseMessageList.add(responseMessage);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param messageList the message list
   * @param message     the message
   * @param cause       the cause
   */
  public WingsException(@NotEmpty List<ResponseMessage> messageList, String message, Throwable cause) {
    super(message, cause);
    responseMessageList = messageList;
  }

  /**
   * @param messageList
   * @param params
   */
  public WingsException(@NotEmpty List<ResponseMessage> messageList, String message, Map<String, Object> params) {
    super(message, null);
    responseMessageList = messageList;
    this.params = params;
  }

  /**
   * Adds the param.
   *
   * @param key   the key
   * @param value the value
   */
  public WingsException addParam(String key, Object value) {
    params.put(key, value);
    return this;
  }

  public enum Scenario {
    // API call scenarios is when the execution was made in the context of API call.
    API_CALL,

    // Scenarios when the execution was in a context of background job.
    BACKGROUND_JOB,
  }

  public static boolean shouldLog(Scenario scenario, Acuteness acuteness) {
    switch (acuteness) {
      case SERIOUS:
        return true;
      case ALERTING:
      case HARMLESS:
        switch (scenario) {
          case API_CALL:
            return false;
          case BACKGROUND_JOB:
            return true;
          default:
            unhandled(scenario);
        }
        break;
      case IGNORABLE:
        switch (scenario) {
          case API_CALL:
            return true;
          case BACKGROUND_JOB:
            return false;
          default:
            unhandled(scenario);
        }
        break;
      default:
        unhandled(acuteness);
    }
    return true;
  }

  public static boolean shouldPropagate(Scenario scenario, Acuteness acuteness) {
    if (scenario != API_CALL) {
      return false;
    }

    switch (acuteness) {
      case SERIOUS:
        return true;
      case ALERTING:
      case HARMLESS:
        return true;
      case IGNORABLE:
        return false;
      default:
        unhandled(acuteness);
    }
    return true;
  }

  public List<ResponseMessage> logProcessedMessages(Scenario scenario) {
    final List<ResponseMessage> responseMessages =
        responseMessageList.stream()
            .map(responseMessage -> ResponseCodeCache.getInstance().rebuildMessage(responseMessage, params))
            .collect(toList());

    if (responseMessages.stream().anyMatch(responseMessage -> shouldLog(scenario, responseMessage.getAcuteness()))) {
      String msg = "Exception occurred: " + getMessage();
      if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == ERROR)) {
        logger.error(msg, this);
      } else if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == WARN)) {
        logger.warn(msg, this);
      } else if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == INFO)) {
        logger.info(msg, this);
      } else {
        logger.debug(msg, this);
      }
    }

    responseMessages.stream()
        .filter(responseMessage -> shouldLog(scenario, responseMessage.getAcuteness()))
        .forEach(responseMessage -> {
          final Level errorType = responseMessage.getLevel();
          switch (errorType) {
            case DEBUG:
              logger.debug(responseMessage.toString());
              break;
            case INFO:
              logger.info(responseMessage.toString());
              break;
            case WARN:
              logger.warn(responseMessage.toString());
              break;
            case ERROR:
              logger.error(responseMessage.toString());
              break;
            default:
              unhandled(errorType);
          }
        });

    return responseMessages.stream()
        .filter(responseMessage -> shouldPropagate(scenario, responseMessage.getAcuteness()))
        .collect(toList());
  }

  // There is only one use of this method. We should reconsider the need of it.
  @Deprecated
  public String getMessagesAsString() {
    final List<ResponseMessage> responseMessages =
        responseMessageList.stream()
            .map(responseMessage -> ResponseCodeCache.getInstance().rebuildMessage(responseMessage, params))
            .collect(toList());

    StringBuilder errorMsgBuilder = new StringBuilder();
    if (isNotEmpty(responseMessages)) {
      responseMessages.stream().forEach(responseMessage -> {
        errorMsgBuilder.append(responseMessage.getMessage());
        errorMsgBuilder.append('.');
      });
    }

    return errorMsgBuilder.toString();
  }
}
