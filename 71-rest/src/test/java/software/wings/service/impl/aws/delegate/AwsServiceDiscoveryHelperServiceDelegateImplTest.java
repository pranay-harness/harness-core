package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.DnsConfig;
import com.amazonaws.services.servicediscovery.model.GetNamespaceResult;
import com.amazonaws.services.servicediscovery.model.GetServiceResult;
import com.amazonaws.services.servicediscovery.model.Namespace;
import com.amazonaws.services.servicediscovery.model.Service;
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

public class AwsServiceDiscoveryHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsServiceDiscoveryHelperServiceDelegateImpl awsServiceDiscoveryHelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetRecordValueForService() {
    AWSServiceDiscovery mockClient = mock(AWSServiceDiscovery.class);
    doReturn(mockClient)
        .when(awsServiceDiscoveryHelperServiceDelegate)
        .getAmazonServiceDiscoveryClient(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList(), eq(false));
    doReturn(new GetServiceResult().withService(
                 new Service().withDnsConfig(new DnsConfig().withNamespaceId("namespaceId")).withName("serviceName")))
        .when(mockClient)
        .getService(any());
    doReturn(new GetNamespaceResult().withNamespace(new Namespace().withName("namespaceName")))
        .when(mockClient)
        .getNamespace(any());
    doNothing().when(mockTracker).trackSDSCall(anyString());
    String val = awsServiceDiscoveryHelperServiceDelegate.getRecordValueForService(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "id");
    assertThat(val).isEqualTo("serviceName.namespaceName");
  }
}
