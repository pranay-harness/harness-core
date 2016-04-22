package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Artifact.Builder.anArtifact;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.Artifact;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.ArtifactService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class ArtifactResourceTest {
  public static final ArtifactService ARTIFACT_SERVICE = mock(ArtifactService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new ArtifactResource(ARTIFACT_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  public static final String APP_ID = "APP_ID";

  public static final Artifact ACTUAL = anArtifact().withApplication(anApplication().withUuid(APP_ID).build()).build();

  @Before
  public void setUp() {
    reset(ARTIFACT_SERVICE);
    when(ARTIFACT_SERVICE.create(any(Artifact.class))).thenReturn(ACTUAL);
    when(ARTIFACT_SERVICE.update(any(Artifact.class))).thenReturn(ACTUAL);
    PageResponse pageResponse = mock(PageResponse.class);
    when(ARTIFACT_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
  }

  @Test
  public void shouldCreateNewArtifact() {
    Artifact artifact = anArtifact().withApplication(anApplication().withUuid(APP_ID).build()).build();

    RestResponse<Artifact> restResponse =
        RESOURCES.client()
            .target("/artifacts/" + APP_ID)
            .request()
            .post(Entity.entity(artifact, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
  }

  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotMatch() {
    Artifact artifact = anArtifact().withApplication(anApplication().withUuid("BAD_APP_ID").build()).build();

    Response response = RESOURCES.client()
                            .target("/artifacts/" + APP_ID)
                            .request()
                            .post(Entity.entity(artifact, MediaType.APPLICATION_JSON));
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.readEntity(new GenericType<RestResponse<Void>>() {}).getResponseMessages()).hasSize(1);
  }
}
