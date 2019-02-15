package io.harness.functional.artifactstream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.template.artifacts.CustomRepositoryMapping.AttributeMapping.builder;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Script;
import software.wings.beans.template.artifacts.CustomRepositoryMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class ArtifactStreamFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private ServiceGenerator serviceGenerator;

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
  @Ignore
  public void shouldCRUDCustomArtifactStream() {
    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);

    GenericType<RestResponse<CustomArtifactStream>> artifactStreamType =
        new GenericType<RestResponse<CustomArtifactStream>>() {

        };

    String name = "Custom Artifact Stream" + System.currentTimeMillis();
    ArtifactStream customArtifactStream =
        CustomArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .name(name)
            .scripts(Arrays.asList(CustomArtifactStream.Script.builder()
                                       .scriptString("echo Hello World!! and echo ${secrets.getValue(My Secret)")
                                       .build()))
            .tags(Arrays.asList())
            .build();

    RestResponse<CustomArtifactStream> restResponse = given()
                                                          .auth()
                                                          .oauth2(bearerToken)
                                                          .queryParam("accountId", application.getAccountId())
                                                          .queryParam("appId", application.getUuid())
                                                          .body(customArtifactStream, ObjectMapperType.GSON)
                                                          .contentType(ContentType.JSON)
                                                          .post("/artifactstreams")
                                                          .as(artifactStreamType.getType());

    ArtifactStream savedArtifactSteam = restResponse.getResource();
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo(name);
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.CUSTOM.name());
    CustomArtifactStream savedCustomArtifactStream = (CustomArtifactStream) savedArtifactSteam;
    assertThat(savedCustomArtifactStream.getScripts()).isNotEmpty();
    Script script = savedCustomArtifactStream.getScripts().stream().findFirst().orElse(null);
    assertThat(script.getScriptString()).isEqualTo("echo Hello World!! and echo ${secrets.getValue(My Secret)");
  }

  @Test
  @Category(FunctionalTests.class)
  @Ignore
  public void shouldCRUDCustomArtifactStreamWithCustomMapping() {
    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);

    GenericType<RestResponse<CustomArtifactStream>> artifactStreamType =
        new GenericType<RestResponse<CustomArtifactStream>>() {

        };

    String scriptString =
        "echo '{\"results\":[{\"buildNo\":\"1.0\",\"metadata\":{\"repository\":\"maven-releases\",\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war\"}}]}' > $ARTIFACT_RESULT_PATH";
    List<CustomRepositoryMapping.AttributeMapping> attributeMapping = new ArrayList<>();
    attributeMapping.add(builder().relativePath("metadata.downloadUrl").mappedAttribute("downloadUrl").build());
    attributeMapping.add(builder().relativePath("metadata.repository").mappedAttribute("repo").build());
    CustomRepositoryMapping mapping = CustomRepositoryMapping.builder()
                                          .artifactRoot("$.results")
                                          .buildNoPath("buildNo")
                                          .artifactAttributes(attributeMapping)
                                          .build();
    String name = "Custom Artifact Stream" + System.currentTimeMillis();
    ArtifactStream customArtifactStream = CustomArtifactStream.builder()
                                              .appId(application.getUuid())
                                              .serviceId(service.getUuid())
                                              .name(name)
                                              .scripts(Arrays.asList(CustomArtifactStream.Script.builder()
                                                                         .scriptString(scriptString)
                                                                         .customRepositoryMapping(mapping)
                                                                         .build()))
                                              .tags(Arrays.asList())
                                              .build();

    RestResponse<CustomArtifactStream> restResponse = given()
                                                          .auth()
                                                          .oauth2(bearerToken)
                                                          .queryParam("accountId", application.getAccountId())
                                                          .queryParam("appId", application.getUuid())
                                                          .body(customArtifactStream, ObjectMapperType.GSON)
                                                          .contentType(ContentType.JSON)
                                                          .post("/artifactstreams")
                                                          .as(artifactStreamType.getType());

    ArtifactStream savedArtifactSteam = restResponse.getResource();
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo(name);
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.CUSTOM.name());
    CustomArtifactStream savedCustomArtifactStream = (CustomArtifactStream) savedArtifactSteam;
    assertThat(savedCustomArtifactStream.getScripts()).isNotEmpty();
    Script script = savedCustomArtifactStream.getScripts().stream().findFirst().orElse(null);
    assertThat(script.getScriptString()).isEqualTo(scriptString);
    assertThat(script.getCustomRepositoryMapping()).isNotNull();
    assertThat(script.getCustomRepositoryMapping().getBuildNoPath()).isEqualTo("buildNo");
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes().size()).isEqualTo(2);
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes())
        .extracting("mappedAttribute")
        .contains("downloadUrl", "repo");

    // Delete custom artifactStream
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("id", savedArtifactSteam.getUuid())
        .delete("/artifactstreams/{id}")
        .then()
        .statusCode(200);

    // Make sure that it is deleted
    restResponse = given()
                       .auth()
                       .oauth2(bearerToken)
                       .queryParam("appId", application.getUuid())
                       .pathParam("streamId", savedArtifactSteam.getUuid())
                       .get("/artifactstreams/{streamId}")
                       .as(artifactStreamType.getType());
    assertThat(restResponse.getResource()).isNull();
  }
}
