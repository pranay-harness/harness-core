package io.harness.batch.processing.cloudevents.aws.ecs.service.intfc;

import software.wings.beans.ce.CEAwsConfig;

public interface AwsAccountService {
  void syncLinkedAccounts(String accountId, String settingId, CEAwsConfig ceAwsConfig);
}
