package io.harness.functional.artifactstream;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.template.artifactsource.CustomRepositoryMapping.AttributeMapping.builder;

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
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Script;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class ArtifactStreamFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
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
  @Owner(developers = UNKNOWN)
  @Category(FunctionalTests.class)
  public void shouldCRUDCustomArtifactStreamWithCustomMapping() {
    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);

    GenericType<RestResponse<CustomArtifactStream>> artifactStreamType =
        new GenericType<RestResponse<CustomArtifactStream>>() {

        };

    String scriptString = "echo '{\n"
        + "  \"items\" : [ {\n"
        + "    \"id\" : \"bWF2ZW4tcmVsZWFzZXM6Yzc3MDE2OWMwYjJlM2VkOGVmMzU2YWE0ZTliMmZlNDY\",\n"
        + "    \"repository\" : \"maven-releases\",\n"
        + "    \"format\" : \"maven2\",\n"
        + "    \"group\" : \"mygroup\",\n"
        + "    \"name\" : \"myartifact\",\n"
        + "    \"version\" : \"1.0\",\n"
        + "    \"assets\" : [ {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.0/myartifact-1.0.war\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjFkYzQxOWYyMGZmNDY4ZjU\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\n"
        + "        \"md5\" : \"d41d8cd98f00b204e9800998ecf8427e\"\n"
        + "      }\n"
        + "    }, {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war.md5\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.0/myartifact-1.0.war.md5\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YmE3YTE1OTYwNzUxZTE4ZjQ\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"67a74306b06d0c01624fe0d0249a570f4d093747\",\n"
        + "        \"md5\" : \"74be16979710d4c4e7c6647856088456\"\n"
        + "      }\n"
        + "    }, {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war.sha1\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.0/myartifact-1.0.war.sha1\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YmUyYzliZWU1NGMwMTllNzI\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"10a34637ad661d98ba3344717656fcc76209c2f8\",\n"
        + "        \"md5\" : \"0144712dd81be0c3d9724f5e56ce6685\"\n"
        + "      }\n"
        + "    } ]\n"
        + "  }, {\n"
        + "    \"id\" : \"bWF2ZW4tcmVsZWFzZXM6Yzc3MDE2OWMwYjJlM2VkODM1NzM2OGI0M2Q0ZGEzZDg\",\n"
        + "    \"repository\" : \"maven-releases\",\n"
        + "    \"format\" : \"maven2\",\n"
        + "    \"group\" : \"mygroup\",\n"
        + "    \"name\" : \"myartifact\",\n"
        + "    \"version\" : \"1.1\",\n"
        + "    \"assets\" : [ {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.1/myartifact-1.1.war\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.1/myartifact-1.1.war\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjY4OTAzOWNmMmMwNDYzNzM\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\n"
        + "        \"md5\" : \"d41d8cd98f00b204e9800998ecf8427e\"\n"
        + "      }\n"
        + "    }, {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.1/myartifact-1.1.war.md5\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.1/myartifact-1.1.war.md5\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjgxNWI2MmE3ODEyODk2ZjM\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"67a74306b06d0c01624fe0d0249a570f4d093747\",\n"
        + "        \"md5\" : \"74be16979710d4c4e7c6647856088456\"\n"
        + "      }\n"
        + "    }, {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.1/myartifact-1.1.war.sha1\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.1/myartifact-1.1.war.sha1\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjZhZjZiMTE3OWE2YzkzYmM\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"10a34637ad661d98ba3344717656fcc76209c2f8\",\n"
        + "        \"md5\" : \"0144712dd81be0c3d9724f5e56ce6685\"\n"
        + "      }\n"
        + "    } ]\n"
        + "  }, {\n"
        + "    \"id\" : \"bWF2ZW4tcmVsZWFzZXM6Yzc3MDE2OWMwYjJlM2VkODhhN2FhMjRiMWY4YzFiMTk\",\n"
        + "    \"repository\" : \"maven-releases\",\n"
        + "    \"format\" : \"maven2\",\n"
        + "    \"group\" : \"mygroup\",\n"
        + "    \"name\" : \"myartifact\",\n"
        + "    \"version\" : \"1.2\",\n"
        + "    \"assets\" : [ {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.2/myartifact-1.2.war\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.2/myartifact-1.2.war\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjI5YmM0MTlkOGY0MTdiMDQ\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\n"
        + "        \"md5\" : \"d41d8cd98f00b204e9800998ecf8427e\"\n"
        + "      }\n"
        + "    }, {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.2/myartifact-1.2.war.md5\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.2/myartifact-1.2.war.md5\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjNhY2ViODdjNWNjOTBiNDY\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"67a74306b06d0c01624fe0d0249a570f4d093747\",\n"
        + "        \"md5\" : \"74be16979710d4c4e7c6647856088456\"\n"
        + "      }\n"
        + "    }, {\n"
        + "      \"downloadUrl\" : \"http://nexus3.harness.io:8081/repository/maven-releases/mygroup/myartifact/1.2/myartifact-1.2.war.sha1\",\n"
        + "      \"path\" : \"mygroup/myartifact/1.2/myartifact-1.2.war.sha1\",\n"
        + "      \"id\" : \"bWF2ZW4tcmVsZWFzZXM6MzZlM2RlYzhkZTUyOGM5YjJiYjFlZjVhNGFlZjJjOWU\",\n"
        + "      \"repository\" : \"maven-releases\",\n"
        + "      \"format\" : \"maven2\",\n"
        + "      \"checksum\" : {\n"
        + "        \"sha1\" : \"10a34637ad661d98ba3344717656fcc76209c2f8\",\n"
        + "        \"md5\" : \"0144712dd81be0c3d9724f5e56ce6685\"\n"
        + "      }\n"
        + "    } ]\n"
        + "  } ],\n"
        + "  \"continuationToken\" : null\n"
        + "}' > $ARTIFACT_RESULT_PATH";
    List<CustomRepositoryMapping.AttributeMapping> attributeMapping = new ArrayList<>();
    attributeMapping.add(builder().relativePath("assets[0].downloadUrl").mappedAttribute("url").build());
    attributeMapping.add(builder().relativePath("repository").mappedAttribute("repository").build());
    attributeMapping.add(builder().relativePath("assets[0].path").mappedAttribute("path").build());
    CustomRepositoryMapping mapping = CustomRepositoryMapping.builder()
                                          .artifactRoot("$.items")
                                          .buildNoPath("version")
                                          .artifactAttributes(attributeMapping)
                                          .build();
    String name = "Custom Artifact Stream - " + System.currentTimeMillis();
    ArtifactStream customArtifactStream = CustomArtifactStream.builder()
                                              .appId(application.getUuid())
                                              .serviceId(service.getUuid())
                                              .name(name)
                                              .scripts(asList(CustomArtifactStream.Script.builder()
                                                                  .scriptString(scriptString)
                                                                  .customRepositoryMapping(mapping)
                                                                  .build()))
                                              .tags(asList())
                                              .build();

    RestResponse<CustomArtifactStream> restResponse = Setup.portal()
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
    assertThat(script.getCustomRepositoryMapping().getBuildNoPath()).isEqualTo("version");
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes().size()).isEqualTo(3);
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes())
        .extracting("mappedAttribute")
        .contains("url", "repository", "path");

    // Delete custom artifactStream
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", application.getUuid())
        .pathParam("id", savedArtifactSteam.getUuid())
        .delete("/artifactstreams/{id}")
        .then()
        .statusCode(200);

    // Make sure that it is deleted
    restResponse = Setup.portal()
                       .auth()
                       .oauth2(bearerToken)
                       .queryParam("appId", application.getUuid())
                       .pathParam("streamId", savedArtifactSteam.getUuid())
                       .get("/artifactstreams/{streamId}")
                       .as(artifactStreamType.getType());
    assertThat(restResponse.getResource()).isNull();
  }
}
