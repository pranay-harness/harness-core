/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
