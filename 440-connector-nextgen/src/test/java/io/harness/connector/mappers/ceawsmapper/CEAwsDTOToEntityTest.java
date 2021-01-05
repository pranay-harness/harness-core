package io.harness.connector.mappers.ceawsmapper;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEAwsDTOToEntityTest extends CategoryTest {
  @InjectMocks CEAwsDTOToEntity ceAwsDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity() {
    final CEAwsConnectorDTO ceAwsConnectorDTO = AWSConnectorTestHelper.createCEAwsConnectorDTO();
    final CEAwsConfig awsConfig = ceAwsDTOToEntity.toConnectorEntity(ceAwsConnectorDTO);
    // TODO (UTSAV): don't forget to mock "region" and "s3Prefix" fetching in CEAwsDTOToEntity
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
        .hasMessageContaining(CEAwsFeatures.CUR.name());
  }
}
