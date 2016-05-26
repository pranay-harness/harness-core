package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;

import com.google.common.collect.Lists;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.Release.ReleaseBuilder;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ReleaseService;

import java.io.IOException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class ReleaseResourceTest extends WingsBaseTest {
  public static final ReleaseService RELEASE_SERVICE = mock(ReleaseService.class);
  public static final AppService APP_SERVICE = mock(AppService.class);

  public static final ReleaseResource releaseResource = new ReleaseResource(RELEASE_SERVICE, APP_SERVICE);
  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(releaseResource).addProvider(WingsExceptionMapper.class).build();

  public static final String APP_ID = "APP_ID";
  public static final String RELEASE_ID = "RELEASE_ID";

  public static final ReleaseBuilder releaseBuilder = aRelease()
                                                          .withReleaseName("REL1")
                                                          .withAppId(APP_ID)
                                                          .withUuid(RELEASE_ID)
                                                          .withDescription("RELEASE 1")
                                                          .withTargetDate(System.currentTimeMillis() + 1000);

  @Rule
  public Verifier collector = new Verifier() {
    @Override
    protected void verify() {
      verifyNoMoreInteractions(APP_SERVICE, RELEASE_SERVICE);
    }
  };

  /**
   * setup for test.
   */
  @Before
  public void setUp() {
    reset(APP_SERVICE, RELEASE_SERVICE);
    when(APP_SERVICE.findByUuid(APP_ID)).thenReturn(anApplication().withUuid(APP_ID).build());
    when(RELEASE_SERVICE.create(any(Release.class))).then(invocation -> invocation.getArgumentAt(0, Release.class));
    when(RELEASE_SERVICE.update(any(Release.class))).then(invocation -> invocation.getArgumentAt(0, Release.class));
    when(RELEASE_SERVICE.softDelete(anyString(), anyString())).then(invocation -> releaseBuilder.but().build());
    when(RELEASE_SERVICE.addArtifactSource(anyString(), anyString(), any(ArtifactSource.class)))
        .thenReturn(releaseBuilder.but().build());
    when(RELEASE_SERVICE.deleteArtifactSource(anyString(), anyString(), anyString()))
        .thenReturn(releaseBuilder.but().build());
    PageResponse<Release> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Lists.newArrayList());
    when(RELEASE_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
  }

  @Test
  public void shouldCreateNewRelease() {
    Release release = releaseBuilder.but().build();

    RestResponse<Release> restResponse =
        RESOURCES.client()
            .target("/releases?appId=" + APP_ID)
            .request()
            .post(Entity.entity(release, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Release>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Release.class);
    verify(APP_SERVICE).findByUuid(APP_ID);
    verify(RELEASE_SERVICE).create(release);
  }

  @Test
  public void shouldUpdateRelease() {
    Release release = releaseBuilder.but().build();

    RestResponse<Release> restResponse =
        RESOURCES.client()
            .target("/releases/" + RELEASE_ID + "?appId=" + APP_ID)
            .request()
            .put(Entity.entity(release, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Release>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Release.class);
    verify(RELEASE_SERVICE).update(releaseBuilder.but().withUuid(RELEASE_ID).build());
  }

  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotExist() {
    Release release = releaseBuilder.but().build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(()
                        -> RESOURCES.client()
                               .target("/releases?appId=BAD_APP_ID")
                               .request()
                               .post(Entity.entity(release, MediaType.APPLICATION_JSON),
                                   new GenericType<RestResponse<Release>>() {}));
    verify(APP_SERVICE).findByUuid("BAD_APP_ID");
  }

  @Test
  public void shouldAddArtifactSource() {
    ArtifactSource jenkinsArtifactSource = aJenkinsArtifactSource().build();
    RestResponse<Release> restResponse = RESOURCES.client()
                                             .target("/releases/" + RELEASE_ID + "/artifactsources?appId=" + APP_ID)
                                             .request()
                                             .post(Entity.entity(jenkinsArtifactSource, MediaType.APPLICATION_JSON),
                                                 new GenericType<RestResponse<Release>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Release.class);
    verify(RELEASE_SERVICE).addArtifactSource(RELEASE_ID, APP_ID, jenkinsArtifactSource);
  }

  @Test
  public void shouldDeleteArtifactSource() {
    RestResponse<Release> restResponse =
        RESOURCES.client()
            .target("/releases/" + RELEASE_ID + "/artifactsources/artifactSource?appId=" + APP_ID)
            .request()
            .delete(new GenericType<RestResponse<Release>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Release.class);
    verify(RELEASE_SERVICE).deleteArtifactSource(RELEASE_ID, APP_ID, "artifactSource");
  }

  @Test
  public void shouldListResource() throws IOException {
    RestResponse<PageResponse<Release>> restResponse =
        RESOURCES.client()
            .target("/releases/?appId=" + APP_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<Release>>>() {});
    PageRequest<Release> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.addFilter("appId", APP_ID, Operator.EQ);
    expectedPageRequest.setOffset("0");
    expectedPageRequest.setLimit("50");
    verify(RELEASE_SERVICE).list(expectedPageRequest);
  }

  @Test
  public void shouldGetRelease() throws IOException {
    RestResponse<Release> restResponse = RESOURCES.client()
                                             .target("/releases/" + RELEASE_ID + "?appId=" + APP_ID)
                                             .request()
                                             .get(new GenericType<RestResponse<Release>>() {});

    verify(RELEASE_SERVICE).get(RELEASE_ID, APP_ID);
  }

  @Test
  public void shouldDeleteRelease() throws IOException {
    RestResponse<Release> restResponse = RESOURCES.client()
                                             .target("/releases/" + RELEASE_ID + "?appId=" + APP_ID)
                                             .request()
                                             .delete(new GenericType<RestResponse<Release>>() {});

    verify(RELEASE_SERVICE).softDelete(RELEASE_ID, APP_ID);
  }
}
