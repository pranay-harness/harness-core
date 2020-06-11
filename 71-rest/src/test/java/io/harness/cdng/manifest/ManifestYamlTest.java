package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ADWAIT;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.state.ManifestListConfig;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.service.OverrideConfig;
import io.harness.cdng.service.ServiceSpec;
import io.harness.rule.Owner;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Optional;

public class ManifestYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testParseManifestsYaml() throws Exception {
    String yaml = "pipeline:\n"
        + "  name: Production Primary Deployment\n"
        + "  identifier: prod_primary_deployment\n"
        + "  stages:\n"
        + "    - stage:\n"
        + "        identifier: deploymentStage1\n"
        + "        name: deployment-stage-1\n"
        + "        type: deployment\n"
        + "        deployment-type: kubernetes\n"
        + "        service:\n"
        + "          displayName: manager\n"
        + "          identifier: manager\n"
        + "          serviceSpec:\n"
        + "            deploymentType: kubernetes\n"
        + "            manifests:\n"
        + "              - manifest:\n"
        + "                  identifier: specManifest\n"
        + "                  k8s:\n"
        + "                    git:\n"
        + "                      connectorId: myGit\n"
        + "                      paths:\n"
        + "                        - spec\n"
        + "                        - namespace/namespaceDev.yaml\n"
        + "                      fetchType: branch\n"
        + "                      fetchValue: master\n"
        + "              - manifest:\n"
        + "                  identifier: valuesManifest\n"
        + "                  k8s:\n"
        + "                    git:\n"
        + "                      connectorId: myGit\n"
        + "                      paths:\n"
        + "                        - values/dev/values.yaml\n"
        + "                      fetchType: commitId\n"
        + "                      fetchValue: 1234\n"
        + "          overrides:\n"
        + "            manifests:\n"
        + "              - manifest:\n"
        + "                  identifier: overrideSpecManifest\n"
        + "                  k8s:\n"
        + "                    git:\n"
        + "                      connectorId: myGit\n"
        + "                      paths:\n"
        + "                        - overrideSpec.yaml\n"
        + "                      fetchType: branch\n"
        + "                      fetchValue: override\n"
        + "\n"
        + "        execution:\n"
        + "          - phase:\n"
        + "              # Name of the phase. REQUIRED\n"
        + "              name:  Deploy\n"
        + "              identifier: phase1\n"
        + "              steps:\n"
        + "                - step:\n"
        + "                    type: http\n"
        + "                    name: \"Rollout Deployment\"\n"
        + "                    identifier: rollout-deployment\n"
        + "                    spec:\n"
        + "                      socketTimeoutMillis: 1000\n"
        + "                      method: GET\n"
        + "                      url: http://www.mocky.io/v2/5ed11ed8350000b8e1ffa2c5\n";

    CDPipeline pipeline = YamlPipelineUtils.read(yaml, CDPipeline.class);
    assertThat(pipeline.getIdentifier()).isEqualTo("prod_primary_deployment");

    StageWrapper stageWrapper = pipeline.getStages().get(0);
    assertThat(stageWrapper instanceof DeploymentStage).isTrue();

    DeploymentStage deploymentStage = (DeploymentStage) stageWrapper;
    ServiceSpec serviceSpec = deploymentStage.getService().getServiceSpec();
    assertThat(serviceSpec).isNotNull();

    assertThat(serviceSpec.getManifests()).isNotNull();
    ManifestListConfig manifestListConfig = serviceSpec.getManifests();
    List<ManifestConfigWrapper> manifestsConfigWrappers = manifestListConfig.getManifests();

    assertThat(manifestsConfigWrappers).isNotEmpty();
    List<ManifestAttributes> manifestAttributes =
        manifestsConfigWrappers.stream().map(ManifestConfigWrapper::getManifestAttributes).collect(toList());

    assertThat(manifestAttributes.size()).isEqualTo(2);

    Optional<ManifestAttributes> attributesOptional =
        manifestAttributes.stream()
            .filter(manifestAttribute -> "specManifest".equals(manifestAttribute.getIdentifier()))
            .findFirst();
    assertThat(attributesOptional.isPresent()).isTrue();
    ManifestAttributes attribute = attributesOptional.get();
    assertThat(attribute.getIdentifier()).isEqualTo("specManifest");
    assertThat(attribute.getKind()).isEqualTo(ManifestType.K8Manifest);
    GitStore storeConfig = (GitStore) attribute.getStoreConfig();
    assertThat(storeConfig.getKind()).isEqualTo(ManifestStoreType.GIT);
    assertThat(storeConfig.getConnectorId()).isEqualTo("myGit");
    assertThat(storeConfig.getPaths()).containsOnly("spec", "namespace/namespaceDev.yaml");
    assertThat(storeConfig.getFetchType()).isEqualTo("branch");
    assertThat(storeConfig.getFetchValue()).isEqualTo("master");

    attributesOptional = manifestAttributes.stream()
                             .filter(manifestAttribute -> "valuesManifest".equals(manifestAttribute.getIdentifier()))
                             .findFirst();
    assertThat(attributesOptional.isPresent()).isTrue();
    attribute = attributesOptional.get();
    assertThat(attribute.getIdentifier()).isEqualTo("valuesManifest");
    assertThat(attribute.getKind()).isEqualTo(ManifestType.K8Manifest);
    storeConfig = (GitStore) attribute.getStoreConfig();
    assertThat(storeConfig.getKind()).isEqualTo(ManifestStoreType.GIT);
    assertThat(storeConfig.getConnectorId()).isEqualTo("myGit");
    assertThat(storeConfig.getPaths()).containsOnly("values/dev/values.yaml");
    assertThat(storeConfig.getFetchType()).isEqualTo("commitId");
    assertThat(storeConfig.getFetchValue()).isEqualTo("1234");

    // Assert Overrides
    OverrideConfig overrides = deploymentStage.getService().getOverrides();
    assertThat(overrides).isNotNull();
    manifestListConfig = overrides.getManifestListConfig();

    attribute = manifestListConfig.getManifests().iterator().next().getManifestAttributes();
    assertThat(attribute.getIdentifier()).isEqualTo("overrideSpecManifest");
    assertThat(attribute.getKind()).isEqualTo(ManifestType.K8Manifest);
    storeConfig = (GitStore) attribute.getStoreConfig();
    assertThat(storeConfig.getKind()).isEqualTo(ManifestStoreType.GIT);
    assertThat(storeConfig.getConnectorId()).isEqualTo("myGit");
    assertThat(storeConfig.getPaths()).containsOnly("overrideSpec.yaml");
    assertThat(storeConfig.getFetchType()).isEqualTo("branch");
    assertThat(storeConfig.getFetchValue()).isEqualTo("override");
  }
}
