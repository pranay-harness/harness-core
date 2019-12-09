package io.harness.eraro;

import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StrSubstitutor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class MessageManager {
  private static final MessageManager instance = new MessageManager();

  private final Properties messages = new Properties();

  public synchronized void addMessages(InputStream in) throws IOException {
    Properties newMessages = new Properties();
    newMessages.load(in);
    messages.putAll(newMessages);
  }

  public static MessageManager getInstance() {
    return instance;
  }

  public String prepareMessage(ErrorCodeName errorCodeName, String exceptionMessage, Map<String, Object> params) {
    String message = messages.getProperty(errorCodeName.getValue());
    if (message == null) {
      logger.error("Response message for error code {} is not provided! Add one in response_messages.properties file.",
          errorCodeName.getValue());
      message = errorCodeName.getValue();
    }
    return prepareMessage(message, exceptionMessage, params);
  }

  private String prepareMessage(String message, String exceptionMessage, Map<String, Object> params) {
    message = StrSubstitutor.replace(message, params);
    if (exceptionMessage != null) {
      message = StrSubstitutor.replace(message, ImmutableMap.of("exception_message", exceptionMessage));
    }
    if (message.matches(".*(\\$\\$)*\\$\\{.*")) {
      logger.info("Insufficient parameter from [{}] in message \"{}\"", String.join(", ", params.keySet()), message);
    }
    return message;
  }
}
