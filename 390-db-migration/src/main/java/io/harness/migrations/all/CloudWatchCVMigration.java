/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.intfc.CloudWatchService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
public class CloudWatchCVMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CloudWatchService cloudWatchService;

  @Override
  public void migrate() {
    List<String> idsToDelete = new ArrayList<>();

    int updated = 0;
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority)
                                 .filter(CVConfigurationKeys.stateType, StateType.CLOUD_WATCH)
                                 .fetch())) {
      while (iterator.hasNext()) {
        try {
          final CloudWatchCVServiceConfiguration cloudWatchCVConfiguration =
              (CloudWatchCVServiceConfiguration) iterator.next();

          log.info("running migration for {} ", cloudWatchCVConfiguration);

          log.info("running migration for {}", AwsNameSpace.ELB);
          if (isNotEmpty(cloudWatchCVConfiguration.getLoadBalancerMetrics())) {
            cloudWatchCVConfiguration.getLoadBalancerMetrics().forEach(
                (loadBalancer, metrics) -> cloudWatchService.setStatisticsAndUnit(AwsNameSpace.ELB, metrics));
          }

          log.info("running migration for {}", AwsNameSpace.ECS);
          if (isNotEmpty(cloudWatchCVConfiguration.getEcsMetrics())) {
            cloudWatchCVConfiguration.getEcsMetrics().forEach(
                (loadBalancer, metrics) -> cloudWatchService.setStatisticsAndUnit(AwsNameSpace.ECS, metrics));
          }

          log.info("running migration for {}", AwsNameSpace.LAMBDA);
          if (isNotEmpty(cloudWatchCVConfiguration.getLambdaFunctionsMetrics())) {
            cloudWatchCVConfiguration.getLambdaFunctionsMetrics().forEach(
                (loadBalancer, metrics) -> cloudWatchService.setStatisticsAndUnit(AwsNameSpace.LAMBDA, metrics));
          }

          log.info("running migration for {}", AwsNameSpace.EC2);
          if (isNotEmpty(cloudWatchCVConfiguration.getEc2Metrics())) {
            cloudWatchService.setStatisticsAndUnit(AwsNameSpace.EC2, cloudWatchCVConfiguration.getEc2Metrics());
          }

          log.info("saving updated config {}", cloudWatchCVConfiguration);
          wingsPersistence.save(cloudWatchCVConfiguration);
          updated++;
        } catch (Exception e) {
          log.info("Error while running migration", e);
        }
      }
    }

    log.info("Complete. updated " + updated + " records.");
  }
}
