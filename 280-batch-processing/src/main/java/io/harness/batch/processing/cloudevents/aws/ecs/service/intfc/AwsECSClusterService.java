package io.harness.batch.processing.cloudevents.aws.ecs.service.intfc;

import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.entities.billing.CECluster;

import software.wings.beans.ce.CEAwsConfig;

import java.util.List;

public interface AwsECSClusterService {
  List<CECluster> getECSCluster(String accountId, String settingId, CEAwsConfig ceAwsConfig);

  void syncCEClusters(CECloudAccount ceCloudAccount);
}
