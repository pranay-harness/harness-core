package software.wings.integration;

import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.EcsService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anubhaw on 12/28/16.
 */
public class EcsServiceIntegrationTest extends WingsBaseTest {
  @Inject private EcsService ecsService;
  private SettingAttribute awsConnectorSetting =
      aSettingAttribute()
          .withValue(anAwsConfig()
                         .withAccessKey("AKIAJLEKM45P4PO5QUFQ")
                         .withSecretKey("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")
                         .build())
          .build();

  // EC2ContainerService-demo-EcsInstanceLc-RCWEK1ATCWXS
  @Test
  public void shouldProvisionNodes() {
    ecsService.provisionNodes(awsConnectorSetting, "EC2ContainerService-demo-EcsInstanceAsg-1TUMY9AGURFZC", 10);
  }

  @Test
  public void shouldCreateAutoScalingGroupAndProvisionNode() {
    Map<String, Object> params = new HashMap<>();
    params.put("availabilityZones", Arrays.asList("us-east-1a", "us-east-1c", "us-east-1d", "us-east-1e"));
    params.put("vpcZoneIdentifiers", "subnet-9725a6bd,subnet-42ddaf34,subnet-64d99b59,subnet-fbe268a3");
    ecsService.provisionNodes(awsConnectorSetting, 5, "EC2ContainerService-demo-EcsInstanceLc-RCWEK1ATCWXS", params);
  }
}
