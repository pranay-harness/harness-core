/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.yaml.ParameterField;
import io.harness.preflight.PreFlightCheckMetadata;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class EntityReferenceExtractorUtilsTest extends CDNGTestBase {
  @Inject EntityReferenceExtractorUtils entityReferenceExtractorUtils;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testExtractReferredEntities() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(ImmutableMap.of("accountId", "ACCOUNT_ID", "orgIdentifier",
                                "ORG_ID", "projectIdentifier", "PROJECT_ID"))
                            .build();
    VisitorTestChild child = VisitorTestChild.builder().name("child").build();
    ConnectorRefChild connectorRefChild =
        ConnectorRefChild.builder().connectorRef(ParameterField.createValueField("connectorRefChild")).build();
    VisitorTestParent parent =
        VisitorTestParent.builder().name("name").visitorTestChild(child).connectorRefChild(connectorRefChild).build();

    Set<EntityDetailProtoDTO> entityDetails = entityReferenceExtractorUtils.extractReferredEntities(ambiance, parent);
    assertThat(entityDetails.size()).isEqualTo(2);

    EntityDetailProtoDTO entity0 = new ArrayList<>(entityDetails).get(0);
    IdentifierRefProtoDTO identifierRef0 = entity0.getIdentifierRef();

    EntityDetailProtoDTO entity1 = new ArrayList<>(entityDetails).get(1);
    IdentifierRefProtoDTO identifierRef1 = entity1.getIdentifierRef();

    assertThat(identifierRef0.getAccountIdentifier().getValue()).isEqualTo("ACCOUNT_ID");
    assertThat(identifierRef0.getOrgIdentifier().getValue()).isEqualTo("ORG_ID");
    assertThat(identifierRef0.getProjectIdentifier().getValue()).isEqualTo("PROJECT_ID");
    assertThat(identifierRef0.getMetadataCount()).isEqualTo(1);
    assertThat(identifierRef0.containsMetadata(PreFlightCheckMetadata.FQN)).isTrue();

    assertThat(identifierRef1.getAccountIdentifier().getValue()).isEqualTo("ACCOUNT_ID");
    assertThat(identifierRef1.getOrgIdentifier().getValue()).isEqualTo("ORG_ID");
    assertThat(identifierRef1.getProjectIdentifier().getValue()).isEqualTo("PROJECT_ID");
    assertThat(identifierRef1.getMetadataCount()).isEqualTo(1);
    assertThat(identifierRef1.containsMetadata(PreFlightCheckMetadata.FQN)).isTrue();

    List<String> identifiers = new ArrayList<>();
    identifiers.add("connectorRefChild");
    identifiers.add("reference");
    assertThat(identifiers.contains(identifierRef0.getIdentifier().getValue())).isTrue();
    assertThat(identifiers.contains(identifierRef1.getIdentifier().getValue())).isTrue();
  }
}
