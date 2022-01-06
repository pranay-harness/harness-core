/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.graphQLAPIs.service;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.GraphQLRestUtils;

import software.wings.beans.Service;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.AzureArtifactsArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetServiceConnectionFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ArtifactStreamService artifactStreamService;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    resetCache(owners.obtainAccount().getUuid());
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
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
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionArtifactory() {
    service = serviceGenerator.ensureGenericArtifactoryTest(seed, owners, "Artifact Source Test-Artifactory");
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
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("This test is Flaky. Need to debug more by the test owner")
  public void testArtifactSourcesInServiceConnectionBamboo() {
    service = serviceGenerator.ensureBambooGenericTest(seed, owners, "Artifact Source Test-Bamboo2");
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

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionJenkins() {
    service = serviceGenerator.ensureJenkinsGenericTest(seed, owners, "Artifact Source Test-Jenkins");
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
    shouldGetServiceConnectionWithJenkinsArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("This test is Flaky. Need to debug more by the test owner")
  public void testArtifactSourcesInServiceConnectionNexusMaven() {
    service = serviceGenerator.ensureNexusMavenGenericTest(seed, owners, "Artifact Source Test-NexusM");
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
    shouldGetServiceConnectionWithNexusMavenArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("This test is Flaky. Need to debug more by the test owner")
  public void testArtifactSourcesInServiceConnectionNexusNpm() {
    service = serviceGenerator.ensureNexusNpmGenericTest(seed, owners, "Artifact Source Test-NexusN");
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
    shouldGetServiceConnectionWithNexusMavenArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionNexusDocker() {
    service = serviceGenerator.ensureNexusDockerGenericTest(seed, owners, "Artifact Source Test-NexusD");
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
    shouldGetServiceConnectionWithNexusMavenArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionAmi() {
    service = serviceGenerator.ensureAmiGenericTest(seed, owners, "Artifact Source Test-Ami");
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
    shouldGetServiceConnectionWithAmiArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionAs3() {
    service = serviceGenerator.ensureAmazonS3GenericTest(seed, owners, "Artifact Source Test-As3");
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
    shouldGetServiceConnectionWithAs3ArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("This test is Flaky. Need to debug more by the test owner")
  public void testArtifactSourcesInServiceConnectionEcr() {
    service = serviceGenerator.ensureEcsTest(seed, owners, "Artifact Source Test-Ecr");
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
    shouldGetServiceConnectionWithEcrArtifactSource(serviceData, artifactStreams);
  }

  @Test
  @Owner(developers = PRABU, intermittent = true)
  @Category(FunctionalTests.class)
  public void testArtifactSourcesInServiceConnectionAzure() {
    service = serviceGenerator.ensureAzureTest(seed, owners, "Artifact Source Test-Acr");
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
    shouldGetServiceConnectionWithAzureArtifactSource(serviceData, artifactStreams);
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

  public void shouldGetServiceConnectionWithJenkinsArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(JenkinsArtifactStream.class);
    JenkinsArtifactStream requiredArtifactStream = (JenkinsArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("__typename")).isEqualTo("JenkinsArtifactSource");
    assertThat(artifactSourceData.get("jenkinsConnectorId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("jobName")).isEqualTo(requiredArtifactStream.getJobname());
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

  public void shouldGetServiceConnectionWithNexusMavenArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactSourceData.get("properties")).isNotNull();
    Map<String, Object> nexusProperties = (Map<String, Object>) artifactSourceData.get("properties");
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(NexusArtifactStream.class);
    NexusArtifactStream requiredArtifactStream = (NexusArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("__typename")).isEqualTo("NexusArtifactSource");
    assertThat(nexusProperties.get("nexusConnectorId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(nexusProperties.get("repository")).isEqualTo(requiredArtifactStream.getJobname());
    assertThat((String) nexusProperties.get("repositoryFormat"))
        .isEqualTo(requiredArtifactStream.getRepositoryFormat().toUpperCase());
    assertThat(nexusProperties.get("artifactId")).isEqualTo(requiredArtifactStream.getArtifactPaths());
    assertThat(nexusProperties.get("groupId")).isEqualTo(requiredArtifactStream.getGroupId());
    assertThat(nexusProperties.get("classifier")).isEqualTo(requiredArtifactStream.getClassifier());
    assertThat(nexusProperties.get("extension")).isEqualTo(requiredArtifactStream.getExtension());
    assertThat(nexusProperties.get("packageName")).isEqualTo(requiredArtifactStream.getPackageName());
    assertThat(nexusProperties.get("dockerImageName")).isEqualTo(requiredArtifactStream.getImageName());
    assertThat(nexusProperties.get("dockerRegistryUrl")).isEqualTo(requiredArtifactStream.getDockerRegistryUrl());
  }

  public void shouldGetServiceConnectionWithAmiArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(AmiArtifactStream.class);
    AmiArtifactStream requiredArtifactStream = (AmiArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("awsCloudProviderId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("__typename")).isEqualTo("AMIArtifactSource");
    assertThat(artifactSourceData.get("region")).isEqualTo(requiredArtifactStream.getRegion());
    assertThat(artifactSourceData.get("awsTags")).isEqualTo(new ArrayList<>());
    assertThat(artifactSourceData.get("amiResourceFilters")).isEqualTo(new ArrayList<>());
  }

  public void shouldGetServiceConnectionWithAs3ArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(AmazonS3ArtifactStream.class);
    AmazonS3ArtifactStream requiredArtifactStream = (AmazonS3ArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("awsCloudProviderId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("__typename")).isEqualTo("AmazonS3ArtifactSource");
    assertThat(artifactSourceData.get("bucket")).isEqualTo(requiredArtifactStream.getJobname());
    assertThat(artifactSourceData.get("artifactPaths")).isEqualTo(requiredArtifactStream.getArtifactPaths());
  }

  public void shouldGetServiceConnectionWithEcrArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(EcrArtifactStream.class);
    EcrArtifactStream requiredArtifactStream = (EcrArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("awsCloudProviderId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("__typename")).isEqualTo("ECRArtifactSource");
    assertThat(artifactSourceData.get("region")).isEqualTo(requiredArtifactStream.getRegion());
    assertThat(artifactSourceData.get("imageName")).isEqualTo(requiredArtifactStream.getImageName());
  }

  public void shouldGetServiceConnectionWithAzureArtifactSource(
      Map<String, Object> serviceData, List<ArtifactStream> artifactStreams) {
    Map<String, Object> artifactSourceData = ((List<Map<String, Object>>) serviceData.get("artifactSources")).get(0);
    assertThat(artifactStreams).isNotEmpty();
    assertThat(artifactStreams.get(0)).isInstanceOf(AzureArtifactsArtifactStream.class);
    AzureArtifactsArtifactStream requiredArtifactStream = (AzureArtifactsArtifactStream) artifactStreams.get(0);
    assertThat(artifactSourceData.get("azureConnectorId")).isEqualTo(requiredArtifactStream.getSettingId());
    assertThat(artifactSourceData.get("__typename")).isEqualTo("AzureArtifactsArtifactSource");
    assertThat(artifactSourceData.get("packageName")).isEqualTo(requiredArtifactStream.getPackageName());
    assertThat(artifactSourceData.get("project")).isEqualTo(requiredArtifactStream.getProject());
    assertThat(artifactSourceData.get("scope")).isEqualTo("ORGANIZATION");
    assertThat(artifactSourceData.get("packageType")).isEqualTo(requiredArtifactStream.getProtocolType());
    assertThat(artifactSourceData.get("feedName")).isEqualTo(requiredArtifactStream.getFeed());
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
    ...on JenkinsArtifactSource{
          jenkinsConnectorId
          jobName
          artifactPaths
        }
    ...on AmazonS3ArtifactSource{
        awsCloudProviderId
        bucket
        artifactPaths
        }
        ...on ECRArtifactSource{
      awsCloudProviderId
      imageName
      region
    }
    ...on AzureArtifactsArtifactSource{
      azureConnectorId
      feedName
      packageName
      packageType
      project
      scope
    }
    ...on AMIArtifactSource{
        region
        awsCloudProviderId
        awsTags{
          key
          value
        }
        amiResourceFilters{
          key
          value
        }
      }
    ...on NexusArtifactSource{
        properties{
          nexusConnectorId
          repository
          repositoryFormat
          ...on NexusMavenProps{
            artifactId
            groupId
            classifier
            extension
          }
        ...on NexusDockerProps{
          dockerImageName
          dockerRegistryUrl
        }
        ...on NexusNpmProps{
          packageName
        }
        }
      }
    }
  }
}*/
        serviceId);
  }
}
