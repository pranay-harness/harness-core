package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Map;

public class AwsIamHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Spy @InjectMocks private AwsIamHelperServiceDelegateImpl awsIamHelperServiceDelegate;

  @Test
  public void testListIAMRoles() {
    AmazonIdentityManagementClient mockClient = mock(AmazonIdentityManagementClient.class);
    doReturn(mockClient)
        .when(awsIamHelperServiceDelegate)
        .getAmazonIdentityManagementClient(anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListRolesResult().withRoles(
                 new Role().withArn("a1").withRoleName("n1"), new Role().withArn("a2").withRoleName("n2")))
        .when(mockClient)
        .listRoles(any());
    Map<String, String> result = awsIamHelperServiceDelegate.listIAMRoles(AwsConfig.builder().build(), emptyList());
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get("a1")).isEqualTo("n1");
    assertThat(result.get("a2")).isEqualTo("n2");
  }

  @Test
  public void testListIamInstanceRoles() {
    AmazonIdentityManagementClient mockClient = mock(AmazonIdentityManagementClient.class);
    doReturn(mockClient)
        .when(awsIamHelperServiceDelegate)
        .getAmazonIdentityManagementClient(anyString(), any(), anyBoolean());
    doReturn(
        new ListInstanceProfilesResult().withInstanceProfiles(new InstanceProfile().withInstanceProfileName("name1"),
            new InstanceProfile().withInstanceProfileName("name2")))
        .when(mockClient)
        .listInstanceProfiles(any());
    List<String> result = awsIamHelperServiceDelegate.listIamInstanceRoles(AwsConfig.builder().build(), emptyList());
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0)).isEqualTo("name1");
    assertThat(result.get(1)).isEqualTo("name2");
  }
}