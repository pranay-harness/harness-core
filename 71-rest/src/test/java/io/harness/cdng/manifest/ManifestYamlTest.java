package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.redesign.services.CustomExecutionUtils;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

public class ManifestYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testParseManifestsYaml() throws Exception {
    String file = CustomExecutionUtils.class.getClassLoader().getResource("cdng/pipeline.yaml").getFile();
    String fileContent = FileUtils.readFileToString(new File(file), "UTF-8");

    CDPipeline pipeline = YamlPipelineUtils.read(fileContent, CDPipeline.class);

    assertThat(pipeline).isNotNull();

    //    assertThat(pipeline.getIdentifier()).isEqualTo("prod_primary_deployment");
    //
    //    StageWrapper stageWrapper = pipeline.getStages().get(0);
    //    assertThat(stageWrapper instanceof DeploymentStage).isTrue();
    //
    //    DeploymentStage deploymentStage = (DeploymentStage) stageWrapper;
    //    ServiceSpec serviceSpec = deploymentStage.getDeployment().getService().getServiceSpec();
    //    assertThat(serviceSpec).isNotNull();
    //
    //    assertThat(serviceSpec.getManifests()).isNotNull();
    //    ManifestListConfig manifestListConfig = serviceSpec.getManifests();
    //    List<ManifestConfigWrapper> manifestsConfigWrappers = manifestListConfig.getManifests();
    //
    //    assertThat(manifestsConfigWrappers).isNotEmpty();
    //    List<ManifestAttributes> manifestAttributes =
    //        manifestsConfigWrappers.stream().map(ManifestConfigWrapper::getManifestAttributes).collect(toList());
    //
    //    assertThat(manifestAttributes.size()).isEqualTo(2);
    //
    //    Optional<ManifestAttributes> attributesOptional =
    //        manifestAttributes.stream()
    //            .filter(manifestAttribute -> "specManifest".equals(manifestAttribute.getIdentifier()))
    //            .findFirst();
    //    assertThat(attributesOptional.isPresent()).isTrue();
    //    ManifestAttributes attribute = attributesOptional.get();
    //    assertThat(attribute.getIdentifier()).isEqualTo("specManifest");
    //    assertThat(attribute.getKind()).isEqualTo(ManifestType.K8Manifest);
    //    assertThat(((ValuesPathProvider) attribute).getValuesPathsToFetch()).containsOnly("namespace/values.yaml");
    //
    //    GitStore storeConfig = (GitStore) attribute.getStoreConfig();
    //
    //    assertThat(storeConfig.getKind()).isEqualTo(ManifestStoreType.GIT);
    //    assertThat(storeConfig.getConnectorId()).isEqualTo("myGit");
    //    assertThat(storeConfig.getFetchType()).isEqualTo("branch");
    //    assertThat(storeConfig.getFetchValue()).isEqualTo("master");
    //    assertThat(storeConfig.getPaths()).containsOnly("spec.yaml");
    //
    //    attributesOptional = manifestAttributes.stream()
    //                             .filter(manifestAttribute ->
    //                             "valuesManifest".equals(manifestAttribute.getIdentifier())) .findFirst();
    //    assertThat(attributesOptional.isPresent()).isTrue();
    //    attribute = attributesOptional.get();
    //    assertThat(attribute.getIdentifier()).isEqualTo("valuesManifest");
    //    assertThat(attribute.getKind()).isEqualTo(ManifestType.K8Manifest);
    //    assertThat(((ValuesPathProvider) attribute).getValuesPathsToFetch()).containsOnly("dev/values.yaml");
    //    storeConfig = (GitStore) attribute.getStoreConfig();
    //    assertThat(storeConfig.getKind()).isEqualTo(ManifestStoreType.GIT);
    //    assertThat(storeConfig.getConnectorId()).isEqualTo("myGit");
    //    assertThat(storeConfig.getFetchType()).isEqualTo("commitId");
    //    assertThat(storeConfig.getFetchValue()).isEqualTo("1234");
    //    assertThat(storeConfig.getPaths()).containsOnly("dev/spec.yaml");
    //
    //    // Assert Overrides
    //    OverrideConfig overrides = deploymentStage.getDeployment().getService().getOverrides();
    //    assertThat(overrides).isNotNull();
    //    manifestListConfig = overrides.getManifestListConfig();
    //
    //    attribute = manifestListConfig.getManifests().iterator().next().getManifestAttributes();
    //    assertThat(attribute.getIdentifier()).isEqualTo("overrideSpecManifest");
    //    assertThat(attribute.getKind()).isEqualTo(ManifestType.K8Manifest);
    //    assertThat(((ValuesPathProvider) attribute).getValuesPathsToFetch()).containsOnly("overrideValues.yaml");
    //
    //    storeConfig = (GitStore) attribute.getStoreConfig();
    //    assertThat(storeConfig.getKind()).isEqualTo(ManifestStoreType.GIT);
    //    assertThat(storeConfig.getConnectorId()).isEqualTo("myGit");
    //    assertThat(storeConfig.getFetchType()).isEqualTo("branch");
    //    assertThat(storeConfig.getFetchValue()).isEqualTo("override");
    //    assertThat(storeConfig.getPaths()).containsOnly("overrideSpec.yaml");
  }
}
