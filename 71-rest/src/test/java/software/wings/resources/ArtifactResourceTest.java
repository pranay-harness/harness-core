package software.wings.resources;

import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Verifier;
import software.wings.beans.artifact.Artifact;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PermitService;
import software.wings.utils.ResourceTestRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ArtifactResourceTest {
  /**
   * The constant ARTIFACT_SERVICE.
   */
  public static final ArtifactService ARTIFACT_SERVICE = mock(ArtifactService.class);
  public static final ArtifactStreamService ARTIFACT_STREAM_SERVICE =
      mock(ArtifactStreamService.class, RETURNS_DEEP_STUBS);
  public static final PermitService PERMIT_SERVICE = mock(PermitService.class, RETURNS_DEEP_STUBS);
  public static final AppService APP_SERVICE = mock(AppService.class);
  public static final AlertService ALERT_SERVICE = mock(AlertService.class);

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .addResource(new ArtifactResource(
              ARTIFACT_SERVICE, ARTIFACT_STREAM_SERVICE, PERMIT_SERVICE, APP_SERVICE, ALERT_SERVICE))
          .addProvider(ConstraintViolationExceptionMapper.class)
          .addProvider(WingsExceptionMapper.class)
          .build();

  /**
   * The constant ACTUAL.
   */
  public static final Artifact ACTUAL = anArtifact().withAppId(APP_ID).build();

  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(ARTIFACT_SERVICE);
    }
  };

  /**
   * The Temp folder.
   */
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder(new File("/tmp"));

  private File tempFile;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    reset(ARTIFACT_SERVICE);
    when(ARTIFACT_SERVICE.create(any(Artifact.class))).thenReturn(ACTUAL);
    when(ARTIFACT_SERVICE.update(any(Artifact.class))).thenReturn(ACTUAL);
    when(ARTIFACT_SERVICE.get(APP_ID, ARTIFACT_ID, true)).thenReturn(ACTUAL);
    when(ARTIFACT_SERVICE.delete(APP_ID, ARTIFACT_ID)).thenReturn(true);

    tempFile = tempFolder.newFile();
    Files.write("Dummy".getBytes(), tempFile);
    when(ARTIFACT_SERVICE.download(APP_ID, ARTIFACT_ID)).thenReturn(tempFile);
    PageResponse<Artifact> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Lists.newArrayList(ACTUAL));
    pageResponse.setTotal(1l);
    when(ARTIFACT_SERVICE.list(any(PageRequest.class), eq(true))).thenReturn(pageResponse);
  }

  /**
   * Should create new artifact.
   */
  @Test
  @Category(UnitTests.class)
  public void shouldCreateNewArtifact() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("BUILD_NO", "5"))
                            .build();
    when(ARTIFACT_STREAM_SERVICE.get(APP_ID, ARTIFACT_STREAM_ID).fetchArtifactDisplayName("5"))
        .thenReturn("DISPLAY_NAME");

    when(ARTIFACT_STREAM_SERVICE.get(APP_ID, ARTIFACT_STREAM_ID).getFailedCronAttempts()).thenReturn(10);

    when(ARTIFACT_STREAM_SERVICE.get(APP_ID, ARTIFACT_STREAM_ID).getUuid()).thenReturn(ARTIFACT_STREAM_ID);

    RestResponse<Artifact> restResponse =
        RESOURCES.client()
            .target("/artifacts?appId=" + APP_ID)
            .request()
            .post(entity(artifact, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
    verify(ARTIFACT_SERVICE).create(artifact);
    verify(ARTIFACT_STREAM_SERVICE).updateFailedCronAttempts(APP_ID, ARTIFACT_STREAM_ID, 0);
    verify(PERMIT_SERVICE).releasePermitByKey(ARTIFACT_STREAM_ID);
  }

  /**
   * Should update artifact.
   */
  @Test
  @Category(UnitTests.class)
  public void shouldUpdateArtifact() {
    Artifact artifact = anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build();

    RestResponse<Artifact> restResponse =
        RESOURCES.client()
            .target("/artifacts/" + ARTIFACT_ID + "?appId=" + APP_ID)
            .request()
            .put(entity(artifact, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
    verify(ARTIFACT_SERVICE).update(artifact);
  }

  /**
   * Should get artifact.
   */
  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifact() {
    RestResponse<Artifact> restResponse = RESOURCES.client()
                                              .target("/artifacts/" + ARTIFACT_ID + "?appId=" + APP_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Artifact>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Artifact.class);
    verify(ARTIFACT_SERVICE).get(APP_ID, ARTIFACT_ID, true);
  }

  /**
   * Should download artifact.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @Category(UnitTests.class)
  public void shouldDownloadArtifact() throws IOException {
    Response restResponse = RESOURCES.client()
                                .target("/artifacts/" + ARTIFACT_ID + "/artifactFile"
                                    + "?appId=" + APP_ID)
                                .request()
                                .get();
    assertThat(restResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + tempFile.getName());
    assertThat(restResponse.getEntity()).isInstanceOf(ByteArrayInputStream.class);
    assertThat(tempFile).hasContent(new String(ByteStreams.toByteArray((InputStream) restResponse.getEntity())));
    verify(ARTIFACT_SERVICE).download(APP_ID, ARTIFACT_ID);
  }

  /**
   * Should list artifact.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @Category(UnitTests.class)
  public void shouldListArtifact() {
    RESOURCES.client()
        .target("/artifacts/?appId=" + APP_ID)
        .request()
        .get(new GenericType<RestResponse<PageResponse<Artifact>>>() {});
    PageRequest<Artifact> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.addFilter("appId", Operator.EQ, APP_ID);
    expectedPageRequest.setOffset("0");
    verify(ARTIFACT_SERVICE).listSortByBuildNo(APP_ID, null, expectedPageRequest);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListArtifactWithServiceId() {
    RESOURCES.client()
        .target("/artifacts/?appId=" + APP_ID + "&serviceId=" + SERVICE_ID)
        .request()
        .get(new GenericType<RestResponse<PageResponse<Artifact>>>() {});
    PageRequest<Artifact> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.addFilter("appId", Operator.EQ, APP_ID);
    expectedPageRequest.setOffset("0");
    verify(ARTIFACT_SERVICE).listSortByBuildNo(APP_ID, SERVICE_ID, expectedPageRequest);
  }
  /**
   * Should delete artifact.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @Category(UnitTests.class)
  public void shouldDeleteArtifact() throws IOException {
    Response response = RESOURCES.client().target("/artifacts/" + ARTIFACT_ID + "?appId=" + APP_ID).request().delete();
    verify(ARTIFACT_SERVICE).delete(APP_ID, ARTIFACT_ID);
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
