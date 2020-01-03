package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.Service;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class AwsEcsHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsEcsHelperServiceDelegateImpl awsEcsHelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListClusters() {
    AmazonECSClient mockClient = mock(AmazonECSClient.class);
    doReturn(mockClient).when(awsEcsHelperServiceDelegate).getAmazonEcsClient(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListClustersResult().withClusterArns("foo/bar")).when(mockClient).listClusters(any());
    doNothing().when(mockTracker).trackECSCall(anyString());
    List<String> result =
        awsEcsHelperServiceDelegate.listClusters(AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0)).isEqualTo("bar");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListServicesForCluster() {
    AmazonECSClient mockClient = mock(AmazonECSClient.class);
    doReturn(mockClient).when(awsEcsHelperServiceDelegate).getAmazonEcsClient(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListServicesResult().withServiceArns("arn0", "arn1")).when(mockClient).listServices(any());
    doReturn(new DescribeServicesResult().withServices(
                 new Service().withServiceArn("arn0"), new Service().withServiceArn("arn1")))
        .when(mockClient)
        .describeServices(any());
    doNothing().when(mockTracker).trackECSCall(anyString());
    List<Service> services = awsEcsHelperServiceDelegate.listServicesForCluster(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "cluster");
    assertThat(services).isNotNull();
    assertThat(services.size()).isEqualTo(2);
  }
}