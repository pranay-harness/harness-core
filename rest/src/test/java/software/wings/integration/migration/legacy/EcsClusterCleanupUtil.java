package software.wings.integration.migration.legacy;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.Service;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.rules.Integration;

import java.util.Collections;
import java.util.List;

/**
 * Script to clean old service instances from ECS.
 * @author brett on 10/1/17
 */
@Integration
@Ignore
public class EcsClusterCleanupUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(EcsClusterCleanupUtil.class);

  @Inject private AwsClusterService awsClusterService;

  // Comment out the following line in WingsTestModule to execute:
  //    bind(AwsHelperService.class).toInstance(mock(AwsHelperService.class));

  // Enter values for the following, then execute.
  private final String accessKey = "ACCESS_KEY";
  private final String secretKey = "SECRET_KEY";
  private final Regions region = Regions.US_EAST_1;
  private final String clusterName = "CLUSTER_NAME";

  private SettingAttribute connectorConfig =
      aSettingAttribute()
          .withValue(AwsConfig.builder().accessKey(accessKey).secretKey(secretKey.toCharArray()).build())
          .build();

  @Test
  public void cleanupOldServices() {
    List<Service> zeroTaskServices =
        awsClusterService.getServices(region.getName(), connectorConfig, Collections.emptyList(), clusterName)
            .stream()
            .filter(s -> s.getDesiredCount() == 0)
            .collect(toList());
    logger.info("Deleting " + zeroTaskServices.size() + " unused services.");
    zeroTaskServices.forEach(s -> {
      String oldServiceName = s.getServiceName();
      logger.info("Deleting " + oldServiceName);
      awsClusterService.deleteService(
          region.getName(), connectorConfig, Collections.emptyList(), clusterName, oldServiceName);
    });
  }
}
