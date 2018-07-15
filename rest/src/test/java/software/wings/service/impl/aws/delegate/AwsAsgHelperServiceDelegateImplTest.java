package software.wings.service.impl.aws.delegate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;
import java.util.List;

public class AwsAsgHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private AwsEc2HelperServiceDelegate mockAwsEc2HelperServiceDelegate;
  @Mock private EncryptionService mockEncryptionService;
  @Spy @InjectMocks private AwsAsgHelperServiceDelegateImpl awsAsgHelperServiceDelegate;

  @Test
  public void testListAutoScalingGroupNames() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    List<AutoScalingGroup> groups = asList(new AutoScalingGroup().withAutoScalingGroupName("name1"),
        new AutoScalingGroup().withAutoScalingGroupName("name2"));
    doReturn(new DescribeAutoScalingGroupsResult().withAutoScalingGroups(groups))
        .when(mockClient)
        .describeAutoScalingGroups(any());
    List<String> result = awsAsgHelperServiceDelegate.listAutoScalingGroupNames(
        AwsConfig.builder().build(), Collections.emptyList(), "us-east-1");
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0)).isEqualTo("name1");
    assertThat(result.get(1)).isEqualTo("name2");
  }

  @Test
  public void testListAutoScalingGroupInstances() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withInstances(new Instance().withInstanceId("id1"));
    DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
        new DescribeAutoScalingGroupsResult().withAutoScalingGroups(autoScalingGroup);
    doReturn(describeAutoScalingGroupsResult).when(mockClient).describeAutoScalingGroups(any());
    doReturn(singletonList(new com.amazonaws.services.ec2.model.Instance().withInstanceId("id1")))
        .when(mockAwsEc2HelperServiceDelegate)
        .listEc2Instances(any(), anyList(), anyList(), anyString());
    List<com.amazonaws.services.ec2.model.Instance> instanceList =
        awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
            AwsConfig.builder().build(), emptyList(), "us-east-1", "name");
    assertThat(instanceList).isNotNull();
    assertThat(instanceList.size()).isEqualTo(1);
    assertThat(instanceList.get(0).getInstanceId()).isEqualTo("id1");
  }
}