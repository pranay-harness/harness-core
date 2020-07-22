package io.harness.functional.graphQLAPIs.service;

import static io.harness.rule.OwnerRule.PRABU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.GraphQLRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;
import java.util.Map;

public class GetServiceConnectionFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ArtifactStreamService artifactStreamService;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionDocker() {
    service = serviceGenerator.ensureK8sTest(seed, owners, "Artifact Source Test-Docker");
    resetCache(service.getAccountId());
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, service.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, service.getAccountId());
    }
    String query = getGraphQLQueryToFetchServiceConnection(service.getUuid());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, service.getAccountId(), query);
    assertThat(response.get("service")).isNotNull();
    Map<String, Object> serviceData = (Map<String, Object>) response.get("service");
    assertThat(serviceData.get("artifactSources")).isNotNull();
    List<ArtifactStream> artifactStreams =
        artifactStreamService.getArtifactStreamsForService(service.getAppId(), service.getUuid());
    shouldGetServiceConnectionWithDockerArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionArtifactory() {
    service = serviceGenerator.ensureFunctionalTest(seed, owners, "Artifact Source Test-Artifactory");
    resetCache(service.getAccountId());
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, service.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, service.getAccountId());
    }
    String query = getGraphQLQueryToFetchServiceConnection(service.getUuid());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, service.getAccountId(), query);
    assertThat(response.get("service")).isNotNull();
    Map<String, Object> serviceData = (Map<String, Object>) response.get("service");
    assertThat(serviceData.get("artifactSources")).isNotNull();
    List<ArtifactStream> artifactStreams =
        artifactStreamService.getArtifactStreamsForService(service.getAppId(), service.getUuid());
    shouldGetServiceConnectionWithArtifactoryArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionBamboo() {
    service = serviceGenerator.ensureBambooGenericTest(seed, owners, "Artifact Source Test-Bamboo");
    resetCache(service.getAccountId());
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, service.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, service.getAccountId());
    }
    String query = getGraphQLQueryToFetchServiceConnection(service.getUuid());
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, service.getAccountId(), query);
    assertThat(response.get("service")).isNotNull();
    Map<String, Object> serviceData = (Map<String, Object>) response.get("service");
    assertThat(serviceData.get("artifactSources")).isNotNull();
    List<ArtifactStream> artifactStreams =
        artifactStreamService.getArtifactStreamsForService(service.getAppId(), service.getUuid());
    shouldGetServiceConnectionWithBambooArtifactSource(serviceData, artifactStreams);
  }

  public void shouldGetServiceConnectionWithDockerArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(DockerArtifactStream.class);
    DockerArtifactStream requiredArtifactStream = (DockerArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("__typename")).isEqualTo("DockerArtifactSource");
    assertThat(artifactSourceData.get("dockerConnectorId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("imageName")).isEqualTo(requiredArtifactStream.getImageName());
    assertThat(artifactSourceData.get("name")).isEqualTo(requiredArtifactStream.getName());
  }

  public void shouldGetServiceConnectionWithBambooArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(BambooArtifactStream.class);
    BambooArtifactStream requiredArtifactStream = (BambooArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("__typename")).isEqualTo("BambooArtifactSource");
    assertThat(artifactSourceData.get("bambooConnectorId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("planKey")).isEqualTo(requiredArtifactStream.getJobname());
    assertThat(artifactSourceData.get("artifactPaths")).isEqualTo(requiredArtifactStream.getArtifactPaths());
  }

  public void shouldGetServiceConnectionWithArtifactoryArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactSourceData.get("properties")).isNotNull();
    Map<String, Object> artifactoryProperties = (Map<String, Object>) artifactSourceData.get("properties");
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(ArtifactoryArtifactStream.class);
    ArtifactoryArtifactStream requiredArtifactStream = (ArtifactoryArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("__typename")).isEqualTo("ArtifactoryArtifactSource");
    assertThat(artifactoryProperties.get("artifactoryConnectorId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactoryProperties.get("repository")).isEqualTo(requiredArtifactStream.getJobname());
    assertThat(artifactoryProperties.get("artifactPath")).isEqualTo(requiredArtifactStream.getArtifactPattern());
  }

  private String getGraphQLQueryToFetchServiceConnection(String serviceId) {
    return $GQL(/*
    query{
service(serviceId:"%s"){
artifactType
 artifactSources{
  name
  createdAt
  __typename
  ...on DockerArtifactSource{
    dockerConnectorId
    imageName
  }
  ...on ArtifactoryArtifactSource{
        properties{
          artifactoryConnectorId
          repository
          ...on ArtifactoryFileProps{
            artifactPath
          }
        }
      }
  ...on BambooArtifactSource{
        bambooConnectorId
        planKey
        artifactPaths
      }
    }
  }
}*/
        serviceId);
  }
}
