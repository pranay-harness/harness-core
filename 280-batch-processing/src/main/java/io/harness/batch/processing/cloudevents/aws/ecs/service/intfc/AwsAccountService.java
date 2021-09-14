/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.intfc;

import software.wings.beans.ce.CEAwsConfig;

public interface AwsAccountService {
  void syncLinkedAccounts(String accountId, String settingId, CEAwsConfig ceAwsConfig);
}
