package io.harness.cdng.manifest.yaml;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcsStoreConfigTest extends CategoryTest {
  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyOverrides() {
    GcsStoreConfig original = GcsStoreConfig.builder()
                                  .connectorRef(ParameterField.createValueField("connector-ref"))
                                  .bucketName(ParameterField.createValueField("bucket-name"))
                                  .folderPath(ParameterField.createValueField("folder-path"))
                                  .build();

    GcsStoreConfig override = GcsStoreConfig.builder()
                                  .connectorRef(ParameterField.createValueField("connector-ref-override"))
                                  .bucketName(ParameterField.createValueField("bucket-name-override"))
                                  .folderPath(ParameterField.createValueField("folder-path-override"))
                                  .build();

    GcsStoreConfig result = (GcsStoreConfig) original.applyOverrides(override);
    assertThat(result.getConnectorRef().getValue()).isEqualTo("connector-ref-override");
    assertThat(result.getBucketName().getValue()).isEqualTo("bucket-name-override");
    assertThat(result.getFolderPath().getValue()).isEqualTo("folder-path-override");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyOverridesEmpty() {
    GcsStoreConfig original = GcsStoreConfig.builder()
                                  .connectorRef(ParameterField.createValueField("connector-ref"))
                                  .bucketName(ParameterField.createValueField("bucket-name"))
                                  .folderPath(ParameterField.createValueField("folder-path"))
                                  .build();

    GcsStoreConfig override = GcsStoreConfig.builder().build();

    GcsStoreConfig result = (GcsStoreConfig) original.applyOverrides(override);
    assertThat(result.getConnectorRef().getValue()).isEqualTo("connector-ref");
    assertThat(result.getBucketName().getValue()).isEqualTo("bucket-name");
    assertThat(result.getFolderPath().getValue()).isEqualTo("folder-path");
  }
}