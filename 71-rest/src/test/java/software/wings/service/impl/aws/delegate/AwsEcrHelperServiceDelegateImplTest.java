package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.model.AuthorizationData;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenResult;
import com.amazonaws.services.ecr.model.Repository;
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

import java.util.Collections;

public class AwsEcrHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsEcrHelperServiceDelegateImpl awsEcrHelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetEcrImageUrl() {
    AmazonECRClient mockClient = mock(AmazonECRClient.class);
    doReturn(mockClient).when(awsEcrHelperServiceDelegate).getAmazonEcrClient(any(), anyString());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeRepositoriesResult().withRepositories(new Repository().withRepositoryUri("uri")))
        .when(mockClient)
        .describeRepositories(any());
    doNothing().when(mockTracker).trackECRCall(anyString());
    String uri = awsEcrHelperServiceDelegate.getEcrImageUrl(
        AwsConfig.builder().build(), Collections.emptyList(), "us-east-1", "imageName");
    assertThat(uri).isEqualTo("uri");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetAmazonEcrAuthToken() {
    AmazonECRClient mockClient = mock(AmazonECRClient.class);
    doReturn(mockClient).when(awsEcrHelperServiceDelegate).getAmazonEcrClient(any(), anyString());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new GetAuthorizationTokenResult().withAuthorizationData(
                 new AuthorizationData().withAuthorizationToken("token")))
        .when(mockClient)
        .getAuthorizationToken(any());
    doNothing().when(mockTracker).trackECRCall(anyString());
    String token = awsEcrHelperServiceDelegate.getAmazonEcrAuthToken(
        AwsConfig.builder().build(), Collections.emptyList(), "account", "us-east-1");
    assertThat(token).isEqualTo("token");
  }
}