package io.harness.ccm.setup.service.intfc;

import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECluster;

import java.util.List;

public interface AwsEKSClusterService {
  List<CECluster> getEKSCluster(String accountId, String settingId, CEAwsConfig ceAwsConfig);
}
