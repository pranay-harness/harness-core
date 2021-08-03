package io.harness.cdng.manifest.yaml;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class S3StoreConfigTest extends CategoryTest {
  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyOverrides() {
    S3StoreConfig origin = S3StoreConfig.builder()
                               .connectorRef(ParameterField.createValueField("connector-ref"))
                               .bucketName(ParameterField.createValueField("bucket-name"))
                               .region(ParameterField.createValueField("region"))
                               .folderPath(ParameterField.createValueField("folder-path"))
                               .build();

    S3StoreConfig override = S3StoreConfig.builder()
                                 .connectorRef(ParameterField.createValueField("connector-ref-override"))
                                 .bucketName(ParameterField.createValueField("bucket-name-override"))
                                 .region(ParameterField.createValueField("region-override"))
                                 .folderPath(ParameterField.createValueField("folder-path-override"))
                                 .build();

    S3StoreConfig result = (S3StoreConfig) origin.applyOverrides(override);
    assertThat(result.getConnectorRef().getValue()).isEqualTo("connector-ref-override");
    assertThat(result.getBucketName().getValue()).isEqualTo("bucket-name-override");
    assertThat(result.getRegion().getValue()).isEqualTo("region-override");
    assertThat(result.getFolderPath().getValue()).isEqualTo("folder-path-override");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyOverridesEmpty() {
    S3StoreConfig origin = S3StoreConfig.builder()
                               .connectorRef(ParameterField.createValueField("connector-ref"))
                               .bucketName(ParameterField.createValueField("bucket-name"))
                               .region(ParameterField.createValueField("region"))
                               .folderPath(ParameterField.createValueField("folder-path"))
                               .build();

    S3StoreConfig override = S3StoreConfig.builder().build();

    S3StoreConfig result = (S3StoreConfig) origin.applyOverrides(override);

    assertThat(result.getConnectorRef().getValue()).isEqualTo("connector-ref");
    assertThat(result.getBucketName().getValue()).isEqualTo("bucket-name");
    assertThat(result.getRegion().getValue()).isEqualTo("region");
    assertThat(result.getFolderPath().getValue()).isEqualTo("folder-path");
  }
}