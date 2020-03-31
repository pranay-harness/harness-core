package io.harness.ccm.setup.service.impl;

import com.google.inject.Inject;

import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.NameValuePair;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECluster;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AwsEKSClusterServiceImpl implements AwsEKSClusterService {
  private final AwsHelperResourceService awsHelperResourceService;
  private final AwsEKSHelperService awsEKSHelperService;

  @Inject
  public AwsEKSClusterServiceImpl(
      AwsHelperResourceService awsHelperResourceService, AwsEKSHelperService awsEKSHelperService) {
    this.awsHelperResourceService = awsHelperResourceService;
    this.awsEKSHelperService = awsEKSHelperService;
  }

  @Override
  public List<CECluster> getEKSCluster(String accountId, String settingId, CEAwsConfig ceAwsConfig) {
    AwsCrossAccountAttributes awsCrossAccountAttributes = ceAwsConfig.getAwsCrossAccountAttributes();
    List<NameValuePair> awsRegions = awsHelperResourceService.getAwsRegions();
    List<CECluster> clusters = new ArrayList<>();
    awsRegions.forEach(awsRegion -> {
      List<String> eksClusters = awsEKSHelperService.listEKSClusters(awsRegion.getValue(), awsCrossAccountAttributes);
      eksClusters.forEach(eksCluster -> {
        CECluster ceCluster = CECluster.builder()
                                  .accountId(accountId)
                                  .clusterName(eksCluster)
                                  .region(awsRegion.getValue())
                                  .infraAccountId(ceAwsConfig.getAwsAccountId())
                                  .infraMasterAccountId(ceAwsConfig.getAwsMasterAccountId())
                                  .parentAccountSettingId(settingId)
                                  .build();
        clusters.add(ceCluster);
      });
    });
    logger.info("EKS clusters {}", clusters);
    return clusters;
  }
}
