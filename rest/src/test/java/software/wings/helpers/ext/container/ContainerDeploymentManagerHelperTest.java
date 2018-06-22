package software.wings.helpers.ext.container;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AwsEc2Service;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

public class ContainerDeploymentManagerHelperTest extends WingsBaseTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private ArtifactStreamService mockArtifactStreamService;
  @Mock private EncryptionService mockEncryptionService;
  @Mock private SecretManager mockSecretManager;
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private AzureHelperService mockAzureHelperService;
  @Mock private EcrService mockEcrService;
  @Mock private EcrClassicService mockEcrClassicService;
  @Mock private ServiceTemplateService mockServiceTemplateService;
  @Mock private DelegateProxyFactory mockDelegateProxyFactory;

  @Inject @InjectMocks private ContainerDeploymentManagerHelper helper;

  @Test
  public void testGetEcrImageUrl() throws Exception {
    AwsEc2Service mockService = mock(AwsEc2Service.class);
    doReturn(mockService).when(mockDelegateProxyFactory).get(eq(AwsEc2Service.class), any());
    doReturn("Image url").when(mockService).getEcrImageUrl(any(), anyList(), anyString(), anyString());
    String ret = (String) invokeMethod(helper, true, "getEcrImageUrl",
        new Object[] {ACCOUNT_ID, AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()),
            "region", EcrArtifactStream.builder().build()});
    assertEquals(ret, "Image url");
  }

  @Test
  public void testGetAmazonEcrAuthToken() throws Exception {
    AwsEc2Service mockService = mock(AwsEc2Service.class);
    doReturn(mockService).when(mockDelegateProxyFactory).get(eq(AwsEc2Service.class), any());
    doReturn("Token").when(mockService).getAmazonEcrAuthToken(any(), anyList(), anyString(), anyString());
    String ret = (String) invokeMethod(helper, true, "getAmazonEcrAuthToken",
        new Object[] {ACCOUNT_ID, AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()),
            "aws account", "region"});
    assertEquals(ret, "Token");
  }
}