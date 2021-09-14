/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filters;

import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.encryption.SecretRefData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.yaml.ParameterField;
import io.harness.preflight.PreFlightCheckMetadata;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class FilterCreatorHelperTest extends CategoryTest {
  private static final String ACCOUNT_IDENTIFIER = "accountId";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectId";
  private static final String FQN = "first.last";

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertToEntityDetailProtoDTO() {
    EntityDetailProtoDTO entityDetailProtoDTO =
        FilterCreatorHelper.convertToEntityDetailProtoDTO(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FQN,
            ParameterField.createValueField("connectorId"), EntityTypeProtoEnum.CONNECTORS);
    Map<String, String> metadata = new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, FQN));
    assertThat(entityDetailProtoDTO.getIdentifierRef().getMetadata()).isEqualTo(metadata);

    assertThat(entityDetailProtoDTO.getIdentifierRef().getAccountIdentifier().getValue()).isEqualTo(ACCOUNT_IDENTIFIER);
    assertThat(entityDetailProtoDTO.getIdentifierRef().getOrgIdentifier().getValue()).isEqualTo(ORG_IDENTIFIER);
    assertThat(entityDetailProtoDTO.getIdentifierRef().getProjectIdentifier().getValue()).isEqualTo(PROJECT_IDENTIFIER);
    assertThat(entityDetailProtoDTO.getIdentifierRef().getIdentifier().getValue()).isEqualTo("connectorId");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertToEntityDetailProtoDTOWithExpression() {
    EntityDetailProtoDTO entityDetailProtoDTO =
        FilterCreatorHelper.convertToEntityDetailProtoDTO(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FQN,
            ParameterField.createExpressionField(true, "connectorId", null, true), EntityTypeProtoEnum.CONNECTORS);
    Map<String, String> metadata = new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, FQN));
    metadata.put(PreFlightCheckMetadata.EXPRESSION, "connectorId");

    assertThat(entityDetailProtoDTO.getIdentifierRef().getMetadata()).isEqualTo(metadata);

    assertThat(entityDetailProtoDTO.getIdentifierRef().getAccountIdentifier().getValue()).isEqualTo(ACCOUNT_IDENTIFIER);
    assertThat(entityDetailProtoDTO.getIdentifierRef().getOrgIdentifier().getValue()).isEqualTo(ORG_IDENTIFIER);
    assertThat(entityDetailProtoDTO.getIdentifierRef().getProjectIdentifier().getValue()).isEqualTo(PROJECT_IDENTIFIER);
    assertThat(entityDetailProtoDTO.getIdentifierRef().getIdentifier().getValue()).isEqualTo("connectorId");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testEmptySecretRef() {
    SecretRefData secretRefData = new SecretRefData("");
    assertThatThrownBy(
        ()
            -> FilterCreatorHelper.convertSecretToEntityDetailProtoDTO(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER,
                PROJECT_IDENTIFIER, FQN, ParameterField.<SecretRefData>builder().value(secretRefData).build()))
        .hasMessage("No value for secret provided at first.last");
  }
}
