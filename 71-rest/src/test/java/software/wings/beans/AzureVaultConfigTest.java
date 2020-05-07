package software.wings.beans;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.cloudprovider.azure.AzureEnvironmentType;

public class AzureVaultConfigTest extends WingsBaseTest {
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetValidationCriteria() {
    AzureVaultConfig azureVaultConfig = AzureVaultConfig.builder().vaultName("test-vault").build();
    String validationCriteria = azureVaultConfig.getValidationCriteria();
    assertThat(validationCriteria).isEqualTo("https://test-vault.vault.azure.net");

    azureVaultConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE_US_GOVERNMENT);
    validationCriteria = azureVaultConfig.getValidationCriteria();
    assertThat(validationCriteria).isEqualTo("https://test-vault.vault.usgovcloudapi.net");

    azureVaultConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE);
    validationCriteria = azureVaultConfig.getValidationCriteria();
    assertThat(validationCriteria).isEqualTo("https://test-vault.vault.azure.net");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetEncryptionServiceUrl() {
    AzureVaultConfig azureVaultConfig = AzureVaultConfig.builder().vaultName("test-vault").build();
    String validationCriteria = azureVaultConfig.getEncryptionServiceUrl();
    assertThat(validationCriteria).isEqualTo("https://test-vault.vault.azure.net");

    azureVaultConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE_US_GOVERNMENT);
    validationCriteria = azureVaultConfig.getEncryptionServiceUrl();
    assertThat(validationCriteria).isEqualTo("https://test-vault.vault.usgovcloudapi.net");

    azureVaultConfig.setAzureEnvironmentType(AzureEnvironmentType.AZURE);
    validationCriteria = azureVaultConfig.getEncryptionServiceUrl();
    assertThat(validationCriteria).isEqualTo("https://test-vault.vault.azure.net");
  }
}
