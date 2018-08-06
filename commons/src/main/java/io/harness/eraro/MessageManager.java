package io.harness.eraro;

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

  public String prepareMessage(ErrorCodeName errorCodeName, Map<String, Object> params) {
    String message = messages.getProperty(errorCodeName.getValue());
    if (message == null) {
      logger.error("Response message for error code {} is not provided!", errorCodeName.getValue());
      message = errorCodeName.getValue();
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
