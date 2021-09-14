/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.ci.beans.entities.BuildNumberDetails;

public interface CIBuildNumberRepositoryCustom {
  BuildNumberDetails increaseBuildNumber(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
