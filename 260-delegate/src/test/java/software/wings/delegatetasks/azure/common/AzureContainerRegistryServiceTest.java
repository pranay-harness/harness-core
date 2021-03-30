package software.wings.delegatetasks.azure.common;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureContainerRegistryServiceTest extends WingsBaseTest {
  private static final String RESOURCE_GROUP_NAME = "resourceGroupName";
  private static final String SUBSCRIPTION_ID = "subscriptionId";
  private static final String AZURE_REGISTRY_NAME = "azureRegistryName";

  @Mock private AzureContainerRegistryClient mockAzureContainerRegistryClient;

  @Spy @InjectMocks AzureContainerRegistryService azureContainerRegistryService;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetContainerRegistryCredentials() {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureContainerRegistryConnectorDTO azureContainerRegistryConnectorDTO = AzureContainerRegistryConnectorDTO.builder()
                                                                                .subscriptionId(SUBSCRIPTION_ID)
                                                                                .azureRegistryName(AZURE_REGISTRY_NAME)
                                                                                .resourceGroupName("")
                                                                                .build();
    RegistryCredentials registryCredentials = mock(RegistryCredentials.class);
    ArgumentCaptor<AzureContainerRegistryClientContext> argumentCaptor =
        ArgumentCaptor.forClass(AzureContainerRegistryClientContext.class);
    doReturn(Optional.of(registryCredentials))
        .when(mockAzureContainerRegistryClient)
        .getContainerRegistryCredentials(argumentCaptor.capture());
    Registry registry = mock(Registry.class);
    doReturn(RESOURCE_GROUP_NAME).when(registry).resourceGroupName();
    doReturn(Optional.of(registry))
        .when(mockAzureContainerRegistryClient)
        .findFirstContainerRegistryByNameOnSubscription(azureConfig, SUBSCRIPTION_ID, AZURE_REGISTRY_NAME);

    RegistryCredentials resultRegistryCredentials =
        azureContainerRegistryService.getContainerRegistryCredentials(azureConfig, azureContainerRegistryConnectorDTO);

    Assertions.assertThat(resultRegistryCredentials).isEqualTo(registryCredentials);
    AzureContainerRegistryClientContext azureContainerRegistryClientContext = argumentCaptor.getValue();
    Assertions.assertThat(azureContainerRegistryClientContext.getResourceGroupName()).isEqualTo(RESOURCE_GROUP_NAME);
    Assertions.assertThat(azureContainerRegistryClientContext.getRegistryName()).isEqualTo(AZURE_REGISTRY_NAME);
    Assertions.assertThat(azureContainerRegistryClientContext.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    Assertions.assertThat(azureContainerRegistryClientContext.getAzureConfig()).isEqualTo(azureConfig);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testNoContainerRegistryCredentials() {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureContainerRegistryConnectorDTO azureContainerRegistryConnectorDTO = AzureContainerRegistryConnectorDTO.builder()
                                                                                .subscriptionId(SUBSCRIPTION_ID)
                                                                                .azureRegistryName(AZURE_REGISTRY_NAME)
                                                                                .resourceGroupName(RESOURCE_GROUP_NAME)
                                                                                .build();
    doReturn(Optional.empty()).when(mockAzureContainerRegistryClient).getContainerRegistryCredentials(any());
    doReturn(Optional.of(Registry.class))
        .when(mockAzureContainerRegistryClient)
        .findFirstContainerRegistryByNameOnSubscription(azureConfig, SUBSCRIPTION_ID, AZURE_REGISTRY_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureContainerRegistryService.getContainerRegistryCredentials(
                            azureConfig, azureContainerRegistryConnectorDTO))
        .withMessageContaining("Not found container registry credentials");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testNoFirstContainerRegistryByName() {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureContainerRegistryConnectorDTO azureContainerRegistryConnectorDTO = AzureContainerRegistryConnectorDTO.builder()
                                                                                .subscriptionId(SUBSCRIPTION_ID)
                                                                                .azureRegistryName(AZURE_REGISTRY_NAME)
                                                                                .resourceGroupName("")
                                                                                .build();

    doReturn(Optional.empty())
        .when(mockAzureContainerRegistryClient)
        .findFirstContainerRegistryByNameOnSubscription(azureConfig, SUBSCRIPTION_ID, AZURE_REGISTRY_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureContainerRegistryService.getContainerRegistryCredentials(
                            azureConfig, azureContainerRegistryConnectorDTO))
        .withMessageContaining("Not found Azure container registry by name");
  }
}
