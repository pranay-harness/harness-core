/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gcp.client;

import com.google.api.services.container.Container;

public interface GcpClient {
  /**
   * Validate default credentials present on the underlying machine
   */
  void validateDefaultCredentials();

  /**
   * Gets a GCP container service using default credentials
   *
   * @return the gke container service
   */
  Container getGkeContainerService();

  Container getGkeContainerService(char[] serviceAccountKey);
}
