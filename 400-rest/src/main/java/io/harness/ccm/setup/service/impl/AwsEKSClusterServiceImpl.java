/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECluster;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;

import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.NameValuePair;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.intfc.AwsHelperResourceService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CE)
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
    log.info("EKS clusters {}", clusters);
    return clusters;
  }
}
