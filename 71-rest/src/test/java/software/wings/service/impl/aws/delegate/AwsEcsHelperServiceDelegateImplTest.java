package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.ListClustersResult;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class AwsEcsHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Spy @InjectMocks private AwsEcsHelperServiceDelegateImpl awsEcsHelperServiceDelegate;

  @Test
  public void testListClusters() {
    AmazonECSClient mockClient = mock(AmazonECSClient.class);
    doReturn(mockClient).when(awsEcsHelperServiceDelegate).getAmazonEcsClient(anyString(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListClustersResult().withClusterArns("foo/bar")).when(mockClient).listClusters(any());
    List<String> result =
        awsEcsHelperServiceDelegate.listClusters(AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0)).isEqualTo("bar");
  }
}