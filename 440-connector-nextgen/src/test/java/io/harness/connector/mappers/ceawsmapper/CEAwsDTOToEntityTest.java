/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.ceawsmapper;

import static io.harness.connector.utils.AWSConnectorTestHelper.createReportDefinition;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.aws.AwsClient;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CEAwsDTOToEntityTest extends CategoryTest {
  @Mock AwsClient awsClient;
  @Spy @InjectMocks io.harness.connector.mappers.ceawsmapper.CEAwsDTOToEntity ceAwsDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    doReturn(Optional.of(createReportDefinition())).when(ceAwsDTOToEntity).getReportDefinition(any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity() {
    final CEAwsConnectorDTO ceAwsConnectorDTO = AWSConnectorTestHelper.createCEAwsConnectorDTO();
    final CEAwsConfig awsConfig = ceAwsDTOToEntity.toConnectorEntity(ceAwsConnectorDTO);

    assertThat(awsConfig).isEqualTo(AWSConnectorTestHelper.createCEAwsConfigEntity());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testToConnectorEntityCURnotEnabled() {
    final CEAwsConnectorDTO ceAwsConnectorDTO = AWSConnectorTestHelper.createCEAwsConnectorDTO();
    ceAwsConnectorDTO.setCurAttributes(null);

    assertThatThrownBy(() -> ceAwsDTOToEntity.toConnectorEntity(ceAwsConnectorDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(CEFeatures.BILLING.name());
  }
}
