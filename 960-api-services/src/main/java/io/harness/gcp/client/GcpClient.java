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
