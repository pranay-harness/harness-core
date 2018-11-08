package io.harness.eraro;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

public class MessageManager {
  private static final Logger logger = LoggerFactory.getLogger(MessageManager.class);

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
      logger.info(MessageFormat.format(
          "Insufficient parameter from [{0}] in message \"{1}\"", String.join(", ", params.keySet()), message));
    }
    return message;
  }
}
