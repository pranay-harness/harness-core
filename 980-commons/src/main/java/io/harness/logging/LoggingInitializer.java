/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import io.harness.eraro.MessageManager;
import io.harness.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingInitializer {
  public static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static boolean initialized;

  /**
   * Initialize logging.
   */
  public static void initializeLogging() {
    if (!initialized) {
      try (InputStream in = LoggingInitializer.class.getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
        MessageManager.getInstance().addMessages(in);
      } catch (IOException exception) {
        throw new WingsException(exception);
      }

      initialized = true;
    }
  }
}
