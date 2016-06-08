package software.wings.common.cache;

import org.apache.commons.lang3.text.StrSubstitutor;
import software.wings.beans.ErrorConstants;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

// TODO: Auto-generated Javadoc

/**
 * The Class ResponseCodeCache.
 */
public class ResponseCodeCache {
  private static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static final ResponseCodeCache instance = new ResponseCodeCache();

  private final Properties messages;

  private ResponseCodeCache() {
    messages = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(RESPONSE_MESSAGE_FILE);
      messages.load(in);
    } catch (IOException exception) {
      throw new WingsException(exception);
    } finally {
      Misc.quietClose(in);
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

  /**
   * Converts error code into ResponseMessage object.
   *
   * @param errorCode errorCode for which message is needed.
   * @return ResponseMessage Object.
   */
  public ResponseMessage getResponseMessage(ErrorConstants errorCode) {
    String message = messages.getProperty(errorCode.getErrorCode());
    if (message == null) {
      return null;
    }
    return getResponseMessage(errorCode, message);
  }

  private ResponseMessage getResponseMessage(ErrorConstants errorCode, String message) {
    ResponseMessage responseMessage = new ResponseMessage();
    responseMessage.setCode(errorCode);
    responseMessage.setMessage(message);
    responseMessage.setErrorType(ResponseTypeEnum.ERROR);
    return responseMessage;
  }

  /**
   * Converts error code and map of key value pairs for substitution into ResponseMessage object .
   *
   * @param errorCode errorCode for which message is needed.
   * @param params    for substituting in ResponseMessage
   * @return ResponseMessage object.
   */
  public ResponseMessage getResponseMessage(ErrorConstants errorCode, Map<String, Object> params) {
    String message = messages.getProperty(errorCode.getErrorCode());
    if (message == null) {
      return null;
    }
    message = StrSubstitutor.replace(message, params);
    return getResponseMessage(errorCode, message);
  }
}
