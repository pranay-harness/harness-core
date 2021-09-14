/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
