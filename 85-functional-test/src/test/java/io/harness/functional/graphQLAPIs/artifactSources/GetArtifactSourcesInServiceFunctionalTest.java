package io.harness.functional.graphQLAPIs.artifactSources;

import static io.harness.rule.OwnerRule.POOJA;
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
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;
import java.util.Map;

public class GetArtifactSourcesInServiceFunctionalTest extends AbstractFunctionalTest {
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
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, service.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, service.getAccountId());
    }
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL, service.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL, service.getAccountId());
    }
  }

  @Test
  @Owner(developers = POOJA)
  @Category(FunctionalTests.class)
  public void shouldGetArtifactByIdWithArtifactSources() {
    String serviceId = service.getUuid();

    String query = getGraphQLQueryToFetchServiceWithArtifactSource(serviceId);
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, service.getAccountId(), query);

    assertThat(response).isNotEmpty();
    assertThat(response.get("service")).isNotNull();
    Map<String, Object> executionData = (Map<String, Object>) response.get("service");
    assertThat(executionData.get("id")).isEqualTo(serviceId);
    assertThat(executionData.get("name")).isEqualTo(service.getName());
    assertThat(executionData.get("artifactSources")).isNotNull();
    List<Map<String, Object>> artifactSource = (List<Map<String, Object>>) executionData.get("artifactSources");

    ArtifactStream artifactStream = artifactStreamService.get(service.getArtifactStreamIds().get(0));
    assertThat(artifactSource.get(0).get("name")).isEqualTo(artifactStream.getName());
    assertThat(artifactSource.get(0).get("id")).isEqualTo(artifactStream.getUuid());
  }

  private String getGraphQLQueryToFetchServiceWithArtifactSource(String serviceId) {
    return $GQL(/*
query{
service(serviceId: "%s"){
name
id
artifactSources {
name
id
__typename
}
}
}*/ serviceId);
  }
}
