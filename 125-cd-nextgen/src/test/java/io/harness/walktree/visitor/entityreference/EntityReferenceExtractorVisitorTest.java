package io.harness.walktree.visitor.entityreference;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class EntityReferenceExtractorVisitorTest extends CDNGTestBase {
  @Inject SimpleVisitorFactory factory;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  @Ignore("New Test in PMS will be written")
  public void testEntityReferenceExtractorVisitor() throws IOException {
    String ACCOUNT = "ACCOUNT";
    String ORG = "ORG";
    String PROJECT = "PROJECT";

    Set<IdentifierRefProtoDTO> expectedReferences = new HashSet<>();
    expectedReferences.add(IdentifierRefProtoDTO.newBuilder()
                               .setScope(ScopeProtoEnum.PROJECT)
                               .setAccountIdentifier(StringValue.of(ACCOUNT))
                               .setOrgIdentifier(StringValue.of(ORG))
                               .setProjectIdentifier(StringValue.of(PROJECT))
                               .setIdentifier(StringValue.of("npQuotecenter"))
                               .build());
    expectedReferences.add(IdentifierRefProtoDTO.newBuilder()
                               .setScope(ScopeProtoEnum.PROJECT)
                               .setAccountIdentifier(StringValue.of(ACCOUNT))
                               .setOrgIdentifier(StringValue.of(ORG))
                               .setProjectIdentifier(StringValue.of(PROJECT))
                               .setIdentifier(StringValue.of("myDocker2"))
                               .build());
    expectedReferences.add(IdentifierRefProtoDTO.newBuilder()
                               .setScope(ScopeProtoEnum.ORG)
                               .setAccountIdentifier(StringValue.of(ACCOUNT))
                               .setOrgIdentifier(StringValue.of(ORG))
                               .setIdentifier(StringValue.of("myGitConnector"))
                               .build());
    expectedReferences.add(IdentifierRefProtoDTO.newBuilder()
                               .setScope(ScopeProtoEnum.ACCOUNT)
                               .setAccountIdentifier(StringValue.of(ACCOUNT))
                               .setIdentifier(StringValue.of("myK8sConnector"))
                               .build());

    ClassLoader classLoader = getClass().getClassLoader();
    String pipelineFilename = "connectorRef-pipeline.yaml";
    String pipelineYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(pipelineFilename)), StandardCharsets.UTF_8);

    NgPipeline pipeline = YamlPipelineUtils.read(pipelineYaml, NgPipeline.class);
    EntityReferenceExtractorVisitor visitor =
        factory.obtainEntityReferenceExtractorVisitor(ACCOUNT, ORG, PROJECT, null);

    visitor.walkElementTree(pipeline);
    Set<EntityDetailProtoDTO> references = visitor.getEntityReferenceSet();
    Set<IdentifierRefProtoDTO> entityReferences =
        references.stream().map(EntityDetailProtoDTO::getIdentifierRef).collect(Collectors.toSet());

    assertThat(entityReferences).isEqualTo(expectedReferences);
  }
}
