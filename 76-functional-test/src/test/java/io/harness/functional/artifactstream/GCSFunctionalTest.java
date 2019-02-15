package io.harness.functional.artifactstream;

import static io.harness.threading.Morpheus.sleep;
import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.GcsArtifactStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class GCSFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;

  private static final String GCS_PROJECT = "exploration-161417";
  private static final String GCS_ARTIFACT = "todolist-v1.0.zip";
  private static final String GCS_BUCKET = "functional-test";

  Application application;
  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void shouldCollectGCSArtifact() {
    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    SettingAttribute gcpCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_EXPLORATION_GCS);

    // List buckets and paths for project
    listBuckets(gcpCloudProvider);
    listArtifactPaths(gcpCloudProvider, GCS_BUCKET);

    // Create an artifact stream with retrieved bucket and artifact
    ArtifactStream savedArtifactSteam = saveAndGetGcsArtifactStream(service, gcpCloudProvider, GCS_ARTIFACT);
    sleep(Duration.ofSeconds(60));

    //  Check if GCS artifact collection success.
    GenericType<RestResponse<PageResponse<Artifact>>> artifactType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};

    RestResponse<PageResponse<Artifact>> artifactResponse = given()
                                                                .auth()
                                                                .oauth2(bearerToken)
                                                                .queryParam("appId", application.getUuid())
                                                                .queryParam("serviceId", service.getUuid())
                                                                .get("/artifacts")
                                                                .as(artifactType.getType());
    assertThat(artifactResponse).isNotNull();
    assertThat(artifactResponse.getResource()).isNotEmpty();

    // Get artifact
    List<Artifact> gcsArtifactList =
        artifactResponse.getResource()
            .stream()
            .filter(artifact -> artifact.getArtifactStreamId().equals(savedArtifactSteam.getUuid()))
            .collect(toList());
    assertThat(gcsArtifactList).isNotEmpty().hasSize(1);
    gcsArtifactList.forEach(artifact -> {
      assertThat(artifact.getArtifactStreamId().equals(savedArtifactSteam.getUuid()));
      assertThat(artifact.getStatus().equals(Status.APPROVED));
      assertThat(artifact.getMetadata().get("bucketName").equals(GCS_BUCKET));
      assertThat(artifact.getMetadata().get("artifactFileName").equals(GCS_ARTIFACT));
    });

    // Clean up all resources
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("id", savedArtifactSteam.getUuid())
        .delete("/artifactstreams/{id}")
        .then()
        .statusCode(200);
  }

  @Test
  @Category(FunctionalTests.class)
  public void shouldCollectGCSArtifactWithRegex() {
    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    SettingAttribute gcpCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_EXPLORATION_GCS);

    // List buckets and paths for project
    listBuckets(gcpCloudProvider);
    listArtifactPaths(gcpCloudProvider, GCS_BUCKET);

    // Create an artifact stream with retrieved bucket and artifact
    String artifactPath = "todolist-v*.zip";
    ArtifactStream savedArtifactSteam = saveAndGetGcsArtifactStream(service, gcpCloudProvider, artifactPath);
    sleep(Duration.ofSeconds(60));

    //  Check if GCS artifact collection success.
    GenericType<RestResponse<PageResponse<Artifact>>> artifactType =
        new GenericType<RestResponse<PageResponse<Artifact>>>() {};

    RestResponse<PageResponse<Artifact>> artifactResponse = given()
                                                                .auth()
                                                                .oauth2(bearerToken)
                                                                .queryParam("appId", application.getUuid())
                                                                .queryParam("serviceId", service.getUuid())
                                                                .get("/artifacts")
                                                                .as(artifactType.getType());
    assertThat(artifactResponse).isNotNull();
    assertThat(artifactResponse.getResource()).isNotEmpty();

    // Get artifact
    List<Artifact> gcsArtifactList =
        artifactResponse.getResource()
            .stream()
            .filter(artifact -> artifact.getArtifactStreamId().equals(savedArtifactSteam.getUuid()))
            .collect(toList());
    assertThat(gcsArtifactList).isNotEmpty();
    gcsArtifactList.forEach(artifact -> {
      assertThat(artifact.getArtifactStreamId().equals(savedArtifactSteam.getUuid()));
      assertThat(artifact.getStatus().equals(Status.APPROVED));
      assertThat(artifact.getMetadata().get("bucketName").equals(GCS_BUCKET));
      assertThat(artifact.getMetadata().get("artifactFileName").matches(artifactPath));
    });

    // Clean up all resources
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("id", savedArtifactSteam.getUuid())
        .delete("/artifactstreams/{id}")
        .then()
        .statusCode(200);
  }

  private void listBuckets(SettingAttribute gcpCloudProvider) {
    // List buckets for project
    GenericType<RestResponse<HashMap<String, String>>> bucketType =
        new GenericType<RestResponse<HashMap<String, String>>>() {};
    RestResponse<HashMap<String, String>> bucketResponse = given()
                                                               .auth()
                                                               .oauth2(bearerToken)
                                                               .queryParam("appId", application.getUuid())
                                                               .queryParam("settingId", gcpCloudProvider.getUuid())
                                                               .queryParam("projectId", GCS_PROJECT)
                                                               .get("/build-sources/buckets")
                                                               .as(bucketType.getType());
    assertThat(bucketResponse).isNotNull();
    assertThat(bucketResponse.getResource()).isNotEmpty();
    assertThat(bucketResponse.getResource().containsKey(GCS_BUCKET));
  }

  private void listArtifactPaths(SettingAttribute gcpCloudProvider, String bucket) {
    // List artifact paths for bucket
    GenericType<RestResponse<ArrayList<String>>> artifactPathType =
        new GenericType<RestResponse<ArrayList<String>>>() {};
    RestResponse<ArrayList<String>> artifactPathsResponse = given()
                                                                .auth()
                                                                .oauth2(bearerToken)
                                                                .queryParam("appId", application.getUuid())
                                                                .queryParam("settingId", gcpCloudProvider.getUuid())
                                                                .queryParam("streamType", ArtifactStreamType.GCS.name())
                                                                .pathParam("bucket", bucket)
                                                                .get("/build-sources/jobs/{bucket}/paths")
                                                                .as(artifactPathType.getType());
    assertThat(artifactPathsResponse).isNotNull();
    assertThat(artifactPathsResponse.getResource()).isNotEmpty();
  }

  private ArtifactStream saveAndGetGcsArtifactStream(
      Service service, SettingAttribute gcpCloudProvider, String artifactPath) {
    GenericType<RestResponse<GcsArtifactStream>> artifactStreamType =
        new GenericType<RestResponse<GcsArtifactStream>>() {};
    GcsArtifactStream gcsArtifactStream = GcsArtifactStream.builder()
                                              .appId(application.getUuid())
                                              .serviceId(service.getUuid())
                                              .artifactPaths(Lists.newArrayList(artifactPath))
                                              .jobname(GCS_BUCKET)
                                              .name("GCS Artifact Stream" + System.currentTimeMillis())
                                              .sourceName(GCS_BUCKET + "/" + artifactPath)
                                              .settingId(gcpCloudProvider.getUuid())
                                              .projectId(GCS_PROJECT)
                                              .autoPopulate(true)
                                              .build();

    RestResponse<GcsArtifactStream> restResponse = given()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", application.getAccountId())
                                                       .queryParam("appId", application.getUuid())
                                                       .body(gcsArtifactStream, ObjectMapperType.GSON)
                                                       .contentType(ContentType.JSON)
                                                       .post("/artifactstreams")
                                                       .as(artifactStreamType.getType());

    ArtifactStream savedArtifactSteam = restResponse.getResource();
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo(gcsArtifactStream.getSourceName());
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.GCS.name());
    return savedArtifactSteam;
  }
}
