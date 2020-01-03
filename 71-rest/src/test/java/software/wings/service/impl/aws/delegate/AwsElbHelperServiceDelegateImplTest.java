package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealth;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Map;

public class AwsElbHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsElbHelperServiceDelegateImpl awsElbHelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListClassicLoadBalancers() {
    com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient mockClassicClient =
        mock(com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient.class);
    doReturn(mockClassicClient).when(awsElbHelperServiceDelegate).getClassicElbClient(any(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult()
                 .withLoadBalancerDescriptions(new LoadBalancerDescription().withLoadBalancerName("lb1"),
                     new LoadBalancerDescription().withLoadBalancerName("lb2")))
        .when(mockClassicClient)
        .describeLoadBalancers(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    List<String> classisLbNames =
        awsElbHelperServiceDelegate.listClassicLoadBalancers(AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(classisLbNames).isNotNull();
    assertThat(classisLbNames.size()).isEqualTo(2);
    assertThat(classisLbNames.get(0)).isEqualTo("lb1");
    assertThat(classisLbNames.get(1)).isEqualTo("lb2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListApplicationLoadBalancers() {
    AmazonElasticLoadBalancingClient mockV2Client = mock(AmazonElasticLoadBalancingClient.class);
    doReturn(mockV2Client).when(awsElbHelperServiceDelegate).getAmazonElasticLoadBalancingClientV2(any(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeLoadBalancersResult().withLoadBalancers(
                 new LoadBalancer().withLoadBalancerName("lb1").withType("application"),
                 new LoadBalancer().withLoadBalancerName("lb2").withType("application"),
                 new LoadBalancer().withLoadBalancerName("lb3").withType("network")))
        .when(mockV2Client)
        .describeLoadBalancers(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    List<AwsLoadBalancerDetails> appLbNames = awsElbHelperServiceDelegate.listApplicationLoadBalancerDetails(
        AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(appLbNames).isNotNull();
    assertThat(appLbNames.size()).isEqualTo(2);
    assertThat(appLbNames.get(0).getName()).isEqualTo("lb1");
    assertThat(appLbNames.get(0).getType()).isEqualTo("application");
    assertThat(appLbNames.get(1).getName()).isEqualTo("lb2");
    assertThat(appLbNames.get(1).getType()).isEqualTo("application");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testListEleasticLoadBalancers() {
    AmazonElasticLoadBalancingClient mockV2Client = mock(AmazonElasticLoadBalancingClient.class);
    doReturn(mockV2Client).when(awsElbHelperServiceDelegate).getAmazonElasticLoadBalancingClientV2(any(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeLoadBalancersResult().withLoadBalancers(
                 new LoadBalancer().withLoadBalancerName("lb1").withType("application"),
                 new LoadBalancer().withLoadBalancerName("lb2").withType("application"),
                 new LoadBalancer().withLoadBalancerName("lb3").withType("network")))
        .when(mockV2Client)
        .describeLoadBalancers(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    List<AwsLoadBalancerDetails> appLbNames = awsElbHelperServiceDelegate.listElasticLoadBalancerDetails(
        AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(appLbNames).isNotNull();
    assertThat(appLbNames.size()).isEqualTo(3);
    assertThat(appLbNames.get(0).getName()).isEqualTo("lb1");
    assertThat(appLbNames.get(0).getType()).isEqualTo("application");
    assertThat(appLbNames.get(1).getName()).isEqualTo("lb2");
    assertThat(appLbNames.get(1).getType()).isEqualTo("application");
    assertThat(appLbNames.get(2).getName()).isEqualTo("lb3");
    assertThat(appLbNames.get(2).getType()).isEqualTo("network");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testListNetworkLoadBalancers() {
    AmazonElasticLoadBalancingClient mockv2Client = mock(AmazonElasticLoadBalancingClient.class);
    doReturn(mockv2Client).when(awsElbHelperServiceDelegate).getAmazonElasticLoadBalancingClientV2(any(), any());
    doReturn(new DescribeLoadBalancersResult().withLoadBalancers(
                 new LoadBalancer().withLoadBalancerName("lb1").withType("application"),
                 new LoadBalancer().withLoadBalancerName("lb2").withType("application"),
                 new LoadBalancer().withLoadBalancerName("lb3").withType("network")))
        .when(mockv2Client)
        .describeLoadBalancers(any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    List<AwsLoadBalancerDetails> appLbNames = awsElbHelperServiceDelegate.listNetworkLoadBalancerDetails(
        AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(appLbNames).isNotNull();
    assertThat(appLbNames.size()).isEqualTo(1);
    assertThat(appLbNames.get(0).getName()).isEqualTo("lb3");
    assertThat(appLbNames.get(0).getType()).isEqualTo("network");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListTargetGroupsForAlb() {
    AmazonElasticLoadBalancingClient mockV2Client = mock(AmazonElasticLoadBalancingClient.class);
    doReturn(mockV2Client).when(awsElbHelperServiceDelegate).getAmazonElasticLoadBalancingClientV2(any(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeTargetGroupsResult().withTargetGroups(
                 new TargetGroup().withTargetGroupArn("a1").withTargetGroupName("n1"),
                 new TargetGroup().withTargetGroupArn("a2").withTargetGroupName("n2")))
        .when(mockV2Client)
        .describeTargetGroups(any());
    doReturn(new DescribeLoadBalancersResult().withLoadBalancers(new LoadBalancer().withLoadBalancerArn("arn1")))
        .when(mockV2Client)
        .describeLoadBalancers(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    Map<String, String> result = awsElbHelperServiceDelegate.listTargetGroupsForAlb(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "lbName");
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get("a1")).isEqualTo("n1");
    assertThat(result.get("a2")).isEqualTo("n2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testAllInstancesRegistered() {
    com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient mockClassicClient =
        mock(com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient.class);
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doReturn(new DescribeInstanceHealthResult().withInstanceStates(
                 new InstanceState().withState("Unknown").withInstanceId("id")))
        .doReturn(new DescribeInstanceHealthResult().withInstanceStates(
            new InstanceState().withState("InService").withInstanceId("id")))
        .when(mockClassicClient)
        .describeInstanceHealth(any());
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    boolean result = awsElbHelperServiceDelegate.allInstancesRegistered(
        mockClassicClient, singletonList("id"), "classicLb", mockCallback);
    assertThat(result).isFalse();
    result = awsElbHelperServiceDelegate.allInstancesRegistered(
        mockClassicClient, singletonList("id"), "classicLb", mockCallback);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testAllTargetsRegistered() {
    AmazonElasticLoadBalancingClient mockV2Client = mock(AmazonElasticLoadBalancingClient.class);
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(new DescribeTargetHealthResult().withTargetHealthDescriptions(
                 new TargetHealthDescription()
                     .withTargetHealth(new TargetHealth().withState("Initial"))
                     .withTarget(new TargetDescription().withId("id"))))
        .doReturn(new DescribeTargetHealthResult().withTargetHealthDescriptions(
            new TargetHealthDescription()
                .withTargetHealth(new TargetHealth().withState("Healthy"))
                .withTarget(new TargetDescription().withId("id"))))
        .when(mockV2Client)
        .describeTargetHealth(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    boolean result = awsElbHelperServiceDelegate.allTargetsRegistered(
        mockV2Client, singletonList("id"), "targetGroup", mockCallback);
    assertThat(result).isFalse();
    result = awsElbHelperServiceDelegate.allTargetsRegistered(
        mockV2Client, singletonList("id"), "targetGroup", mockCallback);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testAllTargetsDeRegistered() {
    AmazonElasticLoadBalancingClient mockV2Client = mock(AmazonElasticLoadBalancingClient.class);
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(new DescribeTargetHealthResult().withTargetHealthDescriptions(
                 new TargetHealthDescription()
                     .withTargetHealth(new TargetHealth().withState("Healthy"))
                     .withTarget(new TargetDescription().withId("id"))))
        .doReturn(new DescribeTargetHealthResult())
        .when(mockV2Client)
        .describeTargetHealth(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    boolean result = awsElbHelperServiceDelegate.allTargetsDeRegistered(
        mockV2Client, singletonList("id"), "targetGroup", mockCallback);
    assertThat(result).isFalse();
    result = awsElbHelperServiceDelegate.allTargetsDeRegistered(
        mockV2Client, singletonList("id"), "targetGroup", mockCallback);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testAllInstancesDeRegistered() {
    com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient mockClassicClient =
        mock(com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient.class);
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doReturn(new DescribeInstanceHealthResult().withInstanceStates(
                 new InstanceState().withState("Healthy").withInstanceId("id")))
        .doReturn(new DescribeInstanceHealthResult())
        .when(mockClassicClient)
        .describeInstanceHealth(any());
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    boolean result = awsElbHelperServiceDelegate.allInstancesDeRegistered(
        mockClassicClient, singletonList("id"), "classicLb", mockCallback);
    assertThat(result).isFalse();
    result = awsElbHelperServiceDelegate.allInstancesDeRegistered(
        mockClassicClient, singletonList("id"), "classicLb", mockCallback);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCloneTargetGroup() {
    AmazonElasticLoadBalancingClient mockV2Client = mock(AmazonElasticLoadBalancingClient.class);
    doReturn(mockV2Client).when(awsElbHelperServiceDelegate).getAmazonElasticLoadBalancingClientV2(any(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeTargetGroupsResult().withTargetGroups(new TargetGroup().withTargetType("ip").withPort(80)))
        .when(mockV2Client)
        .describeTargetGroups(any());
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(new CreateTargetGroupResult().withTargetGroups(new TargetGroup().withTargetGroupArn("arn1")))
        .when(mockV2Client)
        .createTargetGroup(any());
    doNothing().when(mockTracker).trackELBCall(anyString());
    doNothing().when(mockTracker).trackClassicELBCall(anyString());
    awsElbHelperServiceDelegate.cloneTargetGroup(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "arn", "stageTargetGroup");
    verify(mockV2Client).createTargetGroup(any());
  }
}