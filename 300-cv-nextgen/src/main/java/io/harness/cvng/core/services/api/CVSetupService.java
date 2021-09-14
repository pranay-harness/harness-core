/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.CVSetupStatusDTO;

import java.util.List;

public interface CVSetupService {
  CVSetupStatusDTO getSetupStatus(String accountId, String orgIdentifier, String projectIdentifier);

  List<DataSourceType> getSupportedProviders(String accountId, String orgIdentifier, String projectIdentifier);
}
