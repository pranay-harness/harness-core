package io.harness.cdng.provision.terraform;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.rule.Owner;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TerraformDestroyStepInfoTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateParams() {
    TerraformDestroyStepInfo terraformDestroyStepInfo = new TerraformDestroyStepInfo();
    Assertions.assertThatThrownBy(terraformDestroyStepInfo::validateSpecParams)
        .hasMessageContaining("Terraform Step configuration is null");

    TerraformStepConfiguration terraformStepConfiguration = new TerraformStepConfiguration();
    terraformDestroyStepInfo.setTerraformStepConfiguration(terraformStepConfiguration);
    Assertions.assertThatThrownBy(terraformDestroyStepInfo::validateSpecParams)
        .hasMessageContaining("Step Configuration Type is null");

    terraformStepConfiguration.setTerraformStepConfigurationType(TerraformStepConfigurationType.INHERIT_FROM_APPLY);
    terraformDestroyStepInfo.validateSpecParams();

    terraformStepConfiguration.setTerraformStepConfigurationType(TerraformStepConfigurationType.INLINE);
    Assertions.assertThatThrownBy(terraformDestroyStepInfo::validateSpecParams)
        .hasMessageContaining("Spec inside Configuration cannot be null");

    TerraformExecutionData terraformExecutionData = new TerraformExecutionData();
    terraformStepConfiguration.setTerraformExecutionData(terraformExecutionData);
    Assertions.assertThatThrownBy(terraformDestroyStepInfo::validateSpecParams)
        .hasMessageContaining("Config files are null");

    TerraformConfigFilesWrapper terraformConfigFilesWrapper = new TerraformConfigFilesWrapper();
    terraformExecutionData.setTerraformConfigFilesWrapper(terraformConfigFilesWrapper);
    Assertions.assertThatThrownBy(terraformDestroyStepInfo::validateSpecParams)
        .hasMessageContaining("Store cannot be null in Config Files");

    StoreConfigWrapper storeConfigWrapper =
        StoreConfigWrapper.builder().type(StoreConfigType.BITBUCKET).spec(BitbucketStore.builder().build()).build();
    terraformConfigFilesWrapper.setStore(storeConfigWrapper);
    terraformDestroyStepInfo.validateSpecParams();
  }
}
