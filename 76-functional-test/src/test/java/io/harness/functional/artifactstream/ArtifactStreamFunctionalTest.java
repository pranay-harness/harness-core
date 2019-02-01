package io.harness.functional.artifactstream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

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
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Script;

import java.util.Arrays;
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

    ArtifactStream customArtifactStream =
        CustomArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .name("Custom Artifact Stream" + System.currentTimeMillis())
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
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo(CustomArtifactStream.ARTIFACT_SOURCE_NAME);
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.CUSTOM.name());
    CustomArtifactStream savedCustomArtifactStream = (CustomArtifactStream) savedArtifactSteam;
    assertThat(savedCustomArtifactStream.getScripts()).isNotEmpty();
    Script script = savedCustomArtifactStream.getScripts().stream().findFirst().orElse(null);
    assertThat(script.getScriptString()).isEqualTo("echo Hello World!! and echo ${secrets.getValue(My Secret)");
  }
}
