package io.harness.ccm.setup.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECluster;

import software.wings.beans.ce.CEAwsConfig;

import java.util.List;

@OwnedBy(CE)
public interface AwsEKSClusterService {
  List<CECluster> getEKSCluster(String accountId, String settingId, CEAwsConfig ceAwsConfig);
}
