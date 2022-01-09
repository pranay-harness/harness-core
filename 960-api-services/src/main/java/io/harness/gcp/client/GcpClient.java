/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
