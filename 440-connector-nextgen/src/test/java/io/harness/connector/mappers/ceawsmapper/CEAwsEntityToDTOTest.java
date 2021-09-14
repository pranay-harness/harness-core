/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.ceawsmapper;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEAwsEntityToDTOTest extends CategoryTest {
  @InjectMocks CEAwsEntityToDTO ceAwsEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    CEAwsConfig ceAwsConfig = AWSConnectorTestHelper.createCEAwsConfigEntity();
    final CEAwsConnectorDTO ceAwsConnectorDTO = ceAwsEntityToDTO.createConnectorDTO(ceAwsConfig);
    assertThat(ceAwsConnectorDTO).isEqualTo(AWSConnectorTestHelper.createCEAwsConnectorDTO());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCreateConnectorDTOWithCURDisabled() {
    final CEAwsConfig ceAwsConfig = AWSConnectorTestHelper.createCEAwsConfigEntity();
    ceAwsConfig.setCurAttributes(null);
    ceAwsConfig.setFeaturesEnabled(ImmutableList.of(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION));

    final CEAwsConnectorDTO expectedCeAwsConnectorDTO = AWSConnectorTestHelper.createCEAwsConnectorDTO();
    expectedCeAwsConnectorDTO.setCurAttributes(null);
    expectedCeAwsConnectorDTO.setFeaturesEnabled(ImmutableList.of(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION));

    assertThat(ceAwsEntityToDTO.createConnectorDTO(ceAwsConfig)).isEqualTo(expectedCeAwsConnectorDTO);
  }
}
