package io.harness.batch.processing.cloudevents.aws.ecs.service.intfc;

import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECluster;

import java.util.List;

public interface AwsECSClusterService {
  List<CECluster> getECSCluster(String accountId, String settingId, CEAwsConfig ceAwsConfig);

  void syncCEClusters(CECloudAccount ceCloudAccount);
}
