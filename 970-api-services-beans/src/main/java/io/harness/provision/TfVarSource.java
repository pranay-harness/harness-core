/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.provision;

public interface TfVarSource {
  TfVarSourceType getTfVarSourceType();

  enum TfVarSourceType { GIT, SCRIPT_REPOSITORY }
}
