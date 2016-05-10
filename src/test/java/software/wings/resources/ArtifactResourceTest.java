package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Artifact.Builder.anArtifact;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Verifier;
import software.wings.beans.Artifact;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.ArtifactService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
  public static final String ARTIFACT_ID = "ARTIFACT_ID";

  public static final Artifact ACTUAL = anArtifact().withAppId(APP_ID).build();
  private static final String SERVICE_ID = "SERVICE_ID";

  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(ARTIFACT_SERVICE);
    }
  };

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder(new File("/tmp"));

  private File tempFile;
  @Before
  public void setUp() throws IOException {
    reset(ARTIFACT_SERVICE);
    when(ARTIFACT_SERVICE.create(any(Artifact.class))).thenReturn(ACTUAL);
    when(ARTIFACT_SERVICE.update(any(Artifact.class))).thenReturn(ACTUAL);
    when(ARTIFACT_SERVICE.get(APP_ID, ARTIFACT_ID)).thenReturn(ACTUAL);
    tempFile = tempFolder.newFile();
    Files.write("Dummy".getBytes(), tempFile);
    when(ARTIFACT_SERVICE.download(APP_ID, ARTIFACT_ID, SERVICE_ID)).thenReturn(tempFile);
    PageResponse<Artifact> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Lists.newArrayList(ACTUAL));
    pageResponse.setTotal(1);
    when(ARTIFACT_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
  }

  @Test
  public void shouldCreateNewArtifact() {
    Artifact artifact = anArtifact().withAppId(APP_ID).build();

    RestResponse<Artifact> restResponse =
        RESOURCES.client()
            .target("/artifacts?appId=" + APP_ID)
            .request()
            .post(Entity.entity(artifact, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
    verify(ARTIFACT_SERVICE).create(artifact);
  }

  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotMatch() {
    Artifact artifact = anArtifact().withAppId("BAD_APP_ID").build();

    Response response = RESOURCES.client()
                            .target("/artifacts/?appId=" + APP_ID)
                            .request()
                            .post(Entity.entity(artifact, MediaType.APPLICATION_JSON));
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.readEntity(new GenericType<RestResponse<Void>>() {}).getResponseMessages()).hasSize(1);
  }

  @Test
  public void shouldUpdateArtifact() {
    Artifact artifact = anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build();

    RestResponse<Artifact> restResponse =
        RESOURCES.client()
            .target("/artifacts/" + ARTIFACT_ID + "?appId=" + APP_ID)
            .request()
            .put(Entity.entity(artifact, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
    verify(ARTIFACT_SERVICE).update(artifact);
  }

  @Test
  public void shouldGetArtifact() {
    RestResponse<Artifact> restResponse = RESOURCES.client()
                                              .target("/artifacts/" + ARTIFACT_ID + "?appId=" + APP_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
    verify(ARTIFACT_SERVICE).get(APP_ID, ARTIFACT_ID);
  }

  @Test
  public void shouldDownloadArtifact() throws IOException {
    Response restResponse =
        RESOURCES.client()
            .target("/artifacts/" + ARTIFACT_ID + "/artifactFile/" + SERVICE_ID + "?appId=" + APP_ID)
            .request()
            .get();
    assertThat(restResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + tempFile.getName());
    assertThat(restResponse.getEntity()).isInstanceOf(ByteArrayInputStream.class);
    assertThat(tempFile).hasContent(new String(ByteStreams.toByteArray((InputStream) restResponse.getEntity())));
    verify(ARTIFACT_SERVICE).download(APP_ID, ARTIFACT_ID, SERVICE_ID);
  }

  @Test
  public void shouldListArtifact() throws IOException {
    RestResponse<PageResponse<Artifact>> restResponse =
        RESOURCES.client()
            .target("/artifacts/?appId=" + APP_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<Artifact>>>() {});
    PageRequest<Artifact> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.addFilter("appId", APP_ID, Operator.EQ);
    expectedPageRequest.setOffset("0");
    expectedPageRequest.setLimit("50");
    verify(ARTIFACT_SERVICE).list(expectedPageRequest);
  }
}
