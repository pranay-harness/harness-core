/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;

public interface APMDelegateService {
  @DelegateTaskType(TaskType.APM_VALIDATE_CONNECTOR_TASK) boolean validateCollector(APMValidateCollectorConfig config);
  @DelegateTaskType(TaskType.APM_GET_TASK)
  String fetch(APMValidateCollectorConfig config, ThirdPartyApiCallLog apiCallLog);
}
