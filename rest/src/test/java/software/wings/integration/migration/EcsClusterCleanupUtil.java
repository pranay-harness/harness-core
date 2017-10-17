package software.wings.integration.migration;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.Service;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.rules.Integration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Script to clean old service instances from ECS.
 * @author brett on 10/1/17
 */
@Integration
@Ignore
public class EcsClusterCleanupUtil extends WingsBaseTest {
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
    List<Service> zeroTaskServices = awsClusterService.getServices(region.getName(), connectorConfig, clusterName)
                                         .stream()
                                         .filter(s -> s.getDesiredCount() == 0)
                                         .collect(Collectors.toList());
    System.out.println("Deleting " + zeroTaskServices.size() + " unused services.");
    zeroTaskServices.forEach(s -> {
      String oldServiceName = s.getServiceName();
      System.out.println("Deleting " + oldServiceName);
      awsClusterService.deleteService(region.getName(), connectorConfig, clusterName, oldServiceName);
    });
  }
}
