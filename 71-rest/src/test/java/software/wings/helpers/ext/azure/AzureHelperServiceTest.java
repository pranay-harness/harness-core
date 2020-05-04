package software.wings.helpers.ext.azure;

import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static software.wings.beans.cloudprovider.azure.AzureEnvironmentType.AZURE;
import static software.wings.beans.cloudprovider.azure.AzureEnvironmentType.AZURE_US_GOVERNMENT;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachines;
import com.microsoft.azure.management.containerservice.OSType;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.RestException;
import io.harness.category.element.UnitTests;
import io.harness.network.Http;
import io.harness.rule.Owner;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureTagDetails;
import software.wings.beans.cloudprovider.azure.AzureEnvironmentType;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureHelperService.class, Http.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureHelperServiceTest extends WingsBaseTest {
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;
  @Mock private EncryptionService encryptionService;

  @InjectMocks private AzureHelperService azureHelperService;

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateAzureAccountCredential() throws Exception {
    PowerMockito.mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    when(authenticated.withDefaultSubscription()).thenReturn(azure);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
    azureHelperService.validateAzureAccountCredential(azureConfig, emptyList());

    ArgumentCaptor<ApplicationTokenCredentials> captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(1)).authenticate(captor.capture());
    ApplicationTokenCredentials tokenCredentials = captor.getValue();
    assertThat(tokenCredentials.clientId()).isEqualTo("clientId");
    assertThat(tokenCredentials.environment().managementEndpoint()).isEqualTo("https://management.core.windows.net/");

    azureConfig.setAzureEnvironmentType(AZURE);
    azureHelperService.validateAzureAccountCredential(azureConfig, emptyList());
    verify(configurable, times(2)).authenticate(captor.capture());
    tokenCredentials = captor.getValue();
    assertThat(tokenCredentials.clientId()).isEqualTo("clientId");
    assertThat(tokenCredentials.environment().managementEndpoint()).isEqualTo("https://management.core.windows.net/");

    azureConfig.setAzureEnvironmentType(AZURE_US_GOVERNMENT);
    azureHelperService.validateAzureAccountCredential(azureConfig, emptyList());
    verify(configurable, times(3)).authenticate(captor.capture());
    tokenCredentials = captor.getValue();
    assertThat(tokenCredentials.clientId()).isEqualTo("clientId");
    assertThat(tokenCredentials.environment().managementEndpoint())
        .isEqualTo("https://management.core.usgovcloudapi.net/");
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testListTags() throws Exception {
    ApplicationTokenCredentials tokenCredentials = mock(ApplicationTokenCredentials.class);
    whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");

    AzureHelperService spyAzureHelperService = spy(AzureHelperService.class);
    on(spyAzureHelperService).set("encryptionService", encryptionService);

    AzureManagementRestClient azureManagementRestClient = mock(AzureManagementRestClient.class);
    doReturn(azureManagementRestClient).when(spyAzureHelperService).getAzureManagementRestClient(any());
    Call<AzureListTagsResponse> responseCall = (Call<AzureListTagsResponse>) mock(Call.class);
    doReturn(responseCall).when(azureManagementRestClient).listTags(anyString(), anyString());

    AzureListTagsResponse azureListTagsResponse = new AzureListTagsResponse();
    TagDetails tagDetails = new TagDetails();
    tagDetails.setTagName("tagName");
    TagValue tagValue = new TagValue();
    tagValue.setTagValue("tagValue");
    tagDetails.setValues(asList(tagValue));
    azureListTagsResponse.setValue(asList(tagDetails));

    Response<AzureListTagsResponse> response = Response.success(azureListTagsResponse);
    when(responseCall.execute()).thenReturn(response);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
    List<AzureTagDetails> azureTagDetails = spyAzureHelperService.listTags(azureConfig, emptyList(), "subscripId");
    assertThat(azureTagDetails.get(0).getTagName()).isEqualTo("tagName");
    assertThat(azureTagDetails.get(0).getValues()).isEqualTo(asList("tagValue"));

    Set<String> tags = spyAzureHelperService.listTagsBySubscription("subscripId", azureConfig, emptyList());
    assertThat(tags).contains("tagName");
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetAzureBearerAuthToken() throws Exception {
    ApplicationTokenCredentials tokenCredentials = mock(ApplicationTokenCredentials.class);
    whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
    azureHelperService.getAzureBearerAuthToken(azureConfig);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(tokenCredentials).getToken(captor.capture());
    assertThat(captor.getValue()).isEqualTo("https://management.core.windows.net/");

    azureConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE);
    azureHelperService.getAzureBearerAuthToken(azureConfig);
    captor = ArgumentCaptor.forClass(String.class);
    verify(tokenCredentials, times(2)).getToken(captor.capture());
    assertThat(captor.getValue()).isEqualTo("https://management.core.windows.net/");

    azureConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE_US_GOVERNMENT);
    azureHelperService.getAzureBearerAuthToken(azureConfig);
    captor = ArgumentCaptor.forClass(String.class);
    verify(tokenCredentials, times(3)).getToken(captor.capture());
    assertThat(captor.getValue()).isEqualTo("https://management.core.usgovcloudapi.net/");
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetAzureManagementRestClient() {
    PowerMockito.mockStatic(Http.class);
    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
    when(Http.getOkHttpClientBuilder()).thenReturn(clientBuilder);

    azureHelperService.getAzureManagementRestClient(null);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    PowerMockito.verifyStatic(Http.class);
    Http.checkAndGetNonProxyIfApplicable(captor.capture());
    assertThat(captor.getValue()).isEqualTo("https://management.azure.com/");

    azureHelperService.getAzureManagementRestClient(AzureEnvironmentType.AZURE_US_GOVERNMENT);
    captor = ArgumentCaptor.forClass(String.class);
    PowerMockito.verifyStatic(Http.class, times(2));
    Http.checkAndGetNonProxyIfApplicable(captor.capture());
    assertThat(captor.getValue()).isEqualTo("https://management.usgovcloudapi.net/");

    azureHelperService.getAzureManagementRestClient(AzureEnvironmentType.AZURE);
    captor = ArgumentCaptor.forClass(String.class);
    PowerMockito.verifyStatic(Http.class, times(3));
    Http.checkAndGetNonProxyIfApplicable(captor.capture());
    assertThat(captor.getValue()).isEqualTo("https://management.azure.com/");
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testURLInGetAzureClient() throws Exception {
    PowerMockito.mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    when(authenticated.withDefaultSubscription()).thenReturn(azure);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
    azureHelperService.getAzureClient(azureConfig);
    ArgumentCaptor<ApplicationTokenCredentials> captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(1)).authenticate(captor.capture());
    assertThat(captor.getValue().environment().managementEndpoint()).isEqualTo("https://management.core.windows.net/");

    azureConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE_US_GOVERNMENT);
    azureHelperService.getAzureClient(azureConfig);
    captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(2)).authenticate(captor.capture());
    assertThat(captor.getValue().environment().managementEndpoint())
        .isEqualTo("https://management.core.usgovcloudapi.net/");

    azureConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE);
    azureHelperService.getAzureClient(azureConfig);
    captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(3)).authenticate(captor.capture());
    assertThat(captor.getValue().environment().managementEndpoint()).isEqualTo("https://management.core.windows.net/");

    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);
    azureHelperService.getAzureClient(azureConfig, "subscriptionId");
    captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(4)).authenticate(captor.capture());
    assertThat(captor.getValue().environment().managementEndpoint()).isEqualTo("https://management.core.windows.net/");

    azureConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE_US_GOVERNMENT);
    azureHelperService.getAzureClient(azureConfig, "subscriptionId");
    captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(5)).authenticate(captor.capture());
    assertThat(captor.getValue().environment().managementEndpoint())
        .isEqualTo("https://management.core.usgovcloudapi.net/");

    azureConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE);
    azureHelperService.getAzureClient(azureConfig, "subscriptionId");
    captor = ArgumentCaptor.forClass(ApplicationTokenCredentials.class);
    verify(configurable, times(6)).authenticate(captor.capture());
    assertThat(captor.getValue().environment().managementEndpoint()).isEqualTo("https://management.core.windows.net/");
  }

  @Test()
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNPEInListVmsByTagsAndResourceGroup() {
    PowerMockito.mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    when(authenticated.withSubscription("subscriptionId")).thenReturn(azure);
    VirtualMachines mockVirtualMachines = mock(VirtualMachines.class);
    when(azure.virtualMachines()).thenReturn(mockVirtualMachines);
    VirtualMachine virtualMachine = mock(VirtualMachine.class);
    PagedList<VirtualMachine> virtualMachinePagedList = new PagedList<VirtualMachine>() {
      @Override
      public Page<VirtualMachine> nextPage(String nextPageLink) throws RestException {
        return null;
      }
    };
    virtualMachinePagedList.add(virtualMachine);
    when(mockVirtualMachines.listByResourceGroup("resourceGroup")).thenReturn(virtualMachinePagedList);

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
    List<VirtualMachine> virtualMachines = azureHelperService.listVmsByTagsAndResourceGroup(
        azureConfig, emptyList(), "subscriptionId", "resourceGroup", emptyMap(), OSType.LINUX);
    assertThat(virtualMachines).isEmpty();
  }
}
