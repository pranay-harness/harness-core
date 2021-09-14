/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.rule.OwnerRule.ACHYUTH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpStoreConfigTest extends CategoryTest {
  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testApplyOverrides() {
    HttpStoreConfig original =
        HttpStoreConfig.builder().connectorRef(ParameterField.createValueField("connector-ref")).build();

    HttpStoreConfig override =
        HttpStoreConfig.builder().connectorRef(ParameterField.createValueField("connector-ref-override")).build();

    HttpStoreConfig result = (HttpStoreConfig) original.applyOverrides(override);
    assertThat(result.getConnectorRef().getValue()).isEqualTo("connector-ref-override");
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testApplyOverridesEmpty() {
    HttpStoreConfig original =
        HttpStoreConfig.builder().connectorRef(ParameterField.createValueField("connector-ref")).build();

    HttpStoreConfig override = HttpStoreConfig.builder().build();

    HttpStoreConfig result = (HttpStoreConfig) original.applyOverrides(override);
    assertThat(result.getConnectorRef().getValue()).isEqualTo("connector-ref");
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testCloneInternal() {
    HttpStoreConfig original =
        HttpStoreConfig.builder().connectorRef(ParameterField.createValueField("connector-ref")).build();

    HttpStoreConfig originClone = (HttpStoreConfig) original.cloneInternal();

    assertThat(originClone.getConnectorRef().getValue()).isEqualTo("connector-ref");
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testExtractConnectorRefs() {
    HttpStoreConfig original =
        HttpStoreConfig.builder().connectorRef(ParameterField.createValueField("connector-ref")).build();

    assertThat(original.extractConnectorRefs().get("connectorRef").getValue()).isEqualTo("connector-ref");
  }
}
