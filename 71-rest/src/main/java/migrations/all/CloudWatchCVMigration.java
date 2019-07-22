package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.intfc.CloudWatchService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;

import java.util.ArrayList;
import java.util.List;

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

          logger.info("running migration for {} ", cloudWatchCVConfiguration);

          logger.info("running migration for {}", AwsNameSpace.ELB);
          if (isNotEmpty(cloudWatchCVConfiguration.getLoadBalancerMetrics())) {
            cloudWatchCVConfiguration.getLoadBalancerMetrics().forEach(
                (loadBalancer, metrics) -> cloudWatchService.setStatisticsAndUnit(AwsNameSpace.ELB, metrics));
          }

          logger.info("running migration for {}", AwsNameSpace.ECS);
          if (isNotEmpty(cloudWatchCVConfiguration.getEcsMetrics())) {
            cloudWatchCVConfiguration.getEcsMetrics().forEach(
                (loadBalancer, metrics) -> cloudWatchService.setStatisticsAndUnit(AwsNameSpace.ECS, metrics));
          }

          logger.info("running migration for {}", AwsNameSpace.LAMBDA);
          if (isNotEmpty(cloudWatchCVConfiguration.getLambdaFunctionsMetrics())) {
            cloudWatchCVConfiguration.getLambdaFunctionsMetrics().forEach(
                (loadBalancer, metrics) -> cloudWatchService.setStatisticsAndUnit(AwsNameSpace.LAMBDA, metrics));
          }

          logger.info("running migration for {}", AwsNameSpace.EC2);
          if (isNotEmpty(cloudWatchCVConfiguration.getEc2Metrics())) {
            cloudWatchService.setStatisticsAndUnit(AwsNameSpace.EC2, cloudWatchCVConfiguration.getEc2Metrics());
          }

          logger.info("saving updated config {}", cloudWatchCVConfiguration);
          wingsPersistence.save(cloudWatchCVConfiguration);
          updated++;
        } catch (Exception e) {
          logger.info("Error while running migration", e);
        }
      }
    }

    logger.info("Complete. updated " + updated + " records.");
  }
}
