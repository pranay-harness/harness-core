package software.wings.common.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.text.StrSubstitutor;

import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

public class ResponseCodeCache {
  private static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static ResponseCodeCache instance;

  private final Properties messages;

  private ResponseCodeCache() {
    messages = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(RESPONSE_MESSAGE_FILE);
      messages.load(in);
    } catch (IOException e) {
      throw new WingsException(e);
    } finally {
      Misc.quietClose(in);
    }
  }

  public static ResponseCodeCache getInstance() {
    if (instance == null) {
      synchronized (ResponseCodeCache.class) {
        if (instance == null) {
          instance = new ResponseCodeCache();
        }
      }
    }
    return instance;
  }

  public ResponseMessage getResponseMessage(String errorCode) {
    String message = messages.getProperty(errorCode);
    if (message == null) {
      return null;
    }
    return getResponseMessage(errorCode, message);
  }
  public ResponseMessage getResponseMessage(String errorCode, Map<String, Object> params) {
    String message = messages.getProperty(errorCode);
    if (message == null) {
      return null;
    }
    message = StrSubstitutor.replace(message, params);
    return getResponseMessage(errorCode, message);
  }
  private ResponseMessage getResponseMessage(String errorCode, String message) {
    ResponseMessage responseMessage = new ResponseMessage();
    responseMessage.setCode(errorCode);
    responseMessage.setMessage(message);
    responseMessage.setErrorType(ResponseTypeEnum.ERROR);
    return responseMessage;
  }
}
