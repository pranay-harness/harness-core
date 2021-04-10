package io.harness.delegate.beans.connector.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

/**
 * Marker interface for all scm connectors.
 */

@OwnedBy(DX)
public interface ScmConnector {
  void setUrl(String url);
  String getUrl();
}
