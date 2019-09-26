package software.wings.delegatetasks.spotinst.taskhandler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.util.Arrays;

public class SpotInstSetupTaskHandlerTest extends WingsBaseTest {
  @Inject @InjectMocks SpotInstSetupTaskHandler spotInstSetupTaskHandler;
  public final String json = "{\n"
      + "  \"group\": {\n"
      + "    \"name\": \"adwait_11\",\n"
      + "    \"capacity\": {\n"
      + "      \"minimum\": 1,\n"
      + "      \"maximum\": 3,\n"
      + "      \"target\": 1,\n"
      + "      \"unit\": \"instance\"\n"
      + "    },\n"
      + "    \"strategy\": {\n"
      + "      \"risk\": 100,\n"
      + "      \"onDemandCount\": null,\n"
      + "      \"availabilityVsCost\": \"balanced\",\n"
      + "      \"drainingTimeout\": 120,\n"
      + "      \"lifetimePeriod\": \"days\",\n"
      + "      \"fallbackToOd\": true,\n"
      + "      \"scalingStrategy\": {},\n"
      + "      \"persistence\": {},\n"
      + "      \"revertToSpot\": {\n"
      + "        \"performAt\": \"always\"\n"
      + "      }\n"
      + "    },\n"
      + "    \"compute\": {\n"
      + "      \"instanceTypes\": {\n"
      + "        \"ondemand\": \"t2.small\",\n"
      + "        \"spot\": [\n"
      + "          \"m3.medium\",\n"
      + "          \"t2.small\",\n"
      + "          \"t3.small\",\n"
      + "          \"t3a.small\",\n"
      + "          \"t2.medium\",\n"
      + "          \"t3.medium\",\n"
      + "          \"t3a.medium\",\n"
      + "          \"a1.medium\"\n"
      + "        ]\n"
      + "      },\n"
      + "      \"availabilityZones\": [\n"
      + "        {\n"
      + "          \"name\": \"us-east-1a\",\n"
      + "          \"subnetIds\": [\n"
      + "            \"subnet-1f703d78\"\n"
      + "          ]\n"
      + "        },\n"
      + "        {\n"
      + "          \"name\": \"us-east-1b\",\n"
      + "          \"subnetIds\": [\n"
      + "            \"subnet-01bdf52f\"\n"
      + "          ]\n"
      + "        },\n"
      + "        {\n"
      + "          \"name\": \"us-east-1c\",\n"
      + "          \"subnetIds\": [\n"
      + "            \"subnet-33eaf779\"\n"
      + "          ]\n"
      + "        },\n"
      + "        {\n"
      + "          \"name\": \"us-east-1d\",\n"
      + "          \"subnetIds\": [\n"
      + "            \"subnet-c1ce809d\"\n"
      + "          ]\n"
      + "        },\n"
      + "        {\n"
      + "          \"name\": \"us-east-1e\",\n"
      + "          \"subnetIds\": [\n"
      + "            \"subnet-7427b64a\"\n"
      + "          ]\n"
      + "        },\n"
      + "        {\n"
      + "          \"name\": \"us-east-1f\",\n"
      + "          \"subnetIds\": [\n"
      + "            \"subnet-11efe81e\"\n"
      + "          ]\n"
      + "        }\n"
      + "      ],\n"
      + "      \"preferredAvailabilityZones\": null,\n"
      + "      \"product\": \"Linux/UNIX\",\n"
      + "      \"launchSpecification\": {\n"
      + "        \"loadBalancerNames\": null,\n"
      + "        \"loadBalancersConfig\": {\n"
      + "          \"loadBalancers\": [\n"
      + "            {\n"
      + "              \"name\": \"someName\",\n"
      + "              \"arn\": \"arn\",\n"
      + "              \"type\": \"TARGET_GROUP\"\n"
      + "            }\n"
      + "          ]\n"
      + "        },\n"
      + "        \"healthCheckType\": null,\n"
      + "        \"securityGroupIds\": [\n"
      + "          \"sg-d748f48f\"\n"
      + "        ],\n"
      + "        \"monitoring\": false,\n"
      + "        \"ebsOptimized\": false,\n"
      + "        \"imageId\": \"ami-1234\",\n"
      + "        \"keyPair\": \"some_name\",\n"
      + "        \"userData\": null,\n"
      + "        \"shutdownScript\": null,\n"
      + "        \"tenancy\": \"default\"\n"
      + "      },\n"
      + "      \"elasticIps\": null\n"
      + "    },\n"
      + "    \"scaling\": {\n"
      + "      \"up\": null,\n"
      + "      \"down\": null\n"
      + "    },\n"
      + "    \"scheduling\": {\n"
      + "      \"tasks\": null\n"
      + "    },\n"
      + "    \"thirdPartiesIntegration\": {},\n"
      + "    \"multai\": null\n"
      + "  }\n"
      + "}";

  public final String newJsonConfig =
      "{\"group\":{\"name\":\"newName\",\"capacity\":{\"minimum\":0,\"maximum\":0,\"target\":0,"
      + "\"unit\":\"instance\"},"
      + "\"strategy\":{\"risk\":100.0,\"availabilityVsCost\":\"balanced\","
      + "\"drainingTimeout\":120.0,\"lifetimePeriod\":\"days\",\"fallbackToOd\":true,"
      + "\"scalingStrategy\":{},\"persistence\":{},\"revertToSpot\":{\"performAt\":\"always\"}},"
      + "\"compute\":"
      + "{\"instanceTypes\":"
      + "{\"ondemand\":\"t2.small\",\"spot\":[\"m3.medium\",\"t2.small\",\"t3.small\",\"t3a.small\","
      + "\"t2.medium\",\"t3.medium\",\"t3a.medium\",\"a1.medium\"]},"
      + "\"availabilityZones\":[{\"name\":\"us-east-1a\","
      + "\"subnetIds\":[\"subnet-1f703d78\"]},{\"name\":\"us-east-1b\",\"subnetIds\":[\"subnet-01bdf52f\"]}"
      + ",{\"name\":\"us-east-1c\",\"subnetIds\":[\"subnet-33eaf779\"]}"
      + ",{\"name\":\"us-east-1d\",\"subnetIds\":[\"subnet-c1ce809d\"]}"
      + ",{\"name\":\"us-east-1e\",\"subnetIds\":[\"subnet-7427b64a\"]}"
      + ",{\"name\":\"us-east-1f\",\"subnetIds\":[\"subnet-11efe81e\"]}]"
      + ",\"product\":\"Linux/UNIX\","
      + "\"launchSpecification\":"
      + "{\"loadBalancersConfig\":"
      + "{\"loadBalancers\":[{\"name\":\"stageTg1\",\"arn\":\"s_tg1\",\"type\":\"TARGET_GROUP\"},"
      + "{\"name\":\"stageTg2\",\"arn\":\"s_tg2\",\"type\":\"TARGET_GROUP\"}]},"
      + "\"securityGroupIds\":[\"sg-d748f48f\"],"
      + "\"monitoring\":false,\"ebsOptimized\":false,\"imageId\":\"img-123456\","
      + "\"keyPair\":\"some_name\",\"tenancy\":\"default\"}},\"scaling\":{},\"scheduling\":{},"
      + "\"thirdPartiesIntegration\":{}}}";

  @Test
  @Category(UnitTests.class)
  public void testGenerateFinalJson() {
    SpotInstSetupTaskParameters parameters =
        SpotInstSetupTaskParameters.builder()
            .blueGreen(true)
            .image("img-123456")
            .elastiGroupJson(json)
            .awsLoadBalancerConfigs(Arrays.asList(LoadBalancerDetailsForBGDeployment.builder()
                                                      .loadBalancerName("lb1")
                                                      .loadBalancerArn("arn1")
                                                      .prodTargetGroupName("p_tg1")
                                                      .prodTargetGroupName("prodTg1")
                                                      .stageTargetGroupName("stageTg1")
                                                      .stageTargetGroupArn("s_tg1")
                                                      .build(),
                LoadBalancerDetailsForBGDeployment.builder()
                    .loadBalancerName("lb2")
                    .loadBalancerArn("arn2")
                    .prodTargetGroupName("p_tg2")
                    .prodTargetGroupName("prodTg2")
                    .stageTargetGroupName("stageTg2")
                    .stageTargetGroupArn("s_tg2")
                    .build()))
            .build();

    assertThat(spotInstSetupTaskHandler.generateFinalJson(parameters, "newName")).isEqualTo(newJsonConfig);
  }
}
