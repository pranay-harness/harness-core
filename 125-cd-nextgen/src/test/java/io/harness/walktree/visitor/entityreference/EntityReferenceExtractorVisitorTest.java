package io.harness.walktree.visitor.entityreference;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.common.EntityReference;
import io.harness.encryption.Scope;
import io.harness.ng.core.EntityDetail;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class EntityReferenceExtractorVisitorTest extends CDNGBaseTest {
  @Inject SimpleVisitorFactory factory;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testEntityReferenceExtractorVisitor() throws IOException {
    String ACCOUNT = "ACCOUNT";
    String ORG = "ORG";
    String PROJECT = "PROJECT";

    Set<EntityReference> expectedReferences = new HashSet<>();
    expectedReferences.add(IdentifierRef.builder()
                               .scope(Scope.PROJECT)
                               .accountIdentifier(ACCOUNT)
                               .orgIdentifier(ORG)
                               .projectIdentifier(PROJECT)
                               .identifier("npQuotecenter")
                               .build());
    expectedReferences.add(IdentifierRef.builder()
                               .scope(Scope.PROJECT)
                               .accountIdentifier(ACCOUNT)
                               .orgIdentifier(ORG)
                               .projectIdentifier(PROJECT)
                               .identifier("myDocker2")
                               .build());
    expectedReferences.add(IdentifierRef.builder()
                               .scope(Scope.ORG)
                               .accountIdentifier(ACCOUNT)
                               .orgIdentifier(ORG)
                               .projectIdentifier(null)
                               .identifier("myGitConnector")
                               .build());
    expectedReferences.add(IdentifierRef.builder()
                               .scope(Scope.ACCOUNT)
                               .accountIdentifier(ACCOUNT)
                               .orgIdentifier(null)
                               .projectIdentifier(null)
                               .identifier("myK8sConnector")
                               .build());

    ClassLoader classLoader = getClass().getClassLoader();
    String pipelineFilename = "connectorRef-pipeline.yaml";
    String pipelineYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(pipelineFilename)), StandardCharsets.UTF_8);

    NgPipeline pipeline = YamlPipelineUtils.read(pipelineYaml, NgPipeline.class);
    EntityReferenceExtractorVisitor visitor = factory.obtainEntityReferenceExtractorVisitor(ACCOUNT, ORG, PROJECT);

    visitor.walkElementTree(pipeline);
    Set<EntityDetail> references = visitor.getEntityReferenceSet();
    Set<EntityReference> entityReferences =
        references.stream().map(EntityDetail::getEntityRef).collect(Collectors.toSet());

    assertThat(entityReferences).isEqualTo(expectedReferences);
  }
}
