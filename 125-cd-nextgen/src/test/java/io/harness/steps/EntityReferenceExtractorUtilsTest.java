package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.preflight.PreFlightCheckMetadata;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
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
    VisitorTestParent parent = VisitorTestParent.builder().name("name").visitorTestChild(child).build();

    Set<EntityDetailProtoDTO> entityDetails = entityReferenceExtractorUtils.extractReferredEntities(ambiance, parent);
    assertThat(entityDetails.size()).isEqualTo(1);

    EntityDetailProtoDTO entity = new ArrayList<>(entityDetails).get(0);
    assertThat(entity.getType()).isEqualTo(EntityTypeProtoEnum.CONNECTORS);

    IdentifierRefProtoDTO identifierRef = entity.getIdentifierRef();
    assertThat(identifierRef.getAccountIdentifier().getValue()).isEqualTo("ACCOUNT_ID");
    assertThat(identifierRef.getOrgIdentifier().getValue()).isEqualTo("ORG_ID");
    assertThat(identifierRef.getProjectIdentifier().getValue()).isEqualTo("PROJECT_ID");
    assertThat(identifierRef.getIdentifier().getValue()).isEqualTo("reference");
    assertThat(identifierRef.getMetadataCount()).isEqualTo(1);
    assertThat(identifierRef.containsMetadata(PreFlightCheckMetadata.FQN)).isTrue();
  }
}