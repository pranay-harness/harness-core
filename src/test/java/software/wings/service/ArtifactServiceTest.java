// TODO:: ArtifactSource: fix tests

// package software.wings.service;
//
// import static java.util.Arrays.asList;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
// import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
// import static org.mockito.Matchers.any;
// import static org.mockito.Matchers.anyString;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
// import static software.wings.beans.Application.Builder.anApplication;
// import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
// import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
// import static software.wings.beans.artifact.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
// import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;
// import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactSource;
// import static software.wings.beans.Release.Builder.aRelease;
// import static software.wings.beans.Service.Builder.aService;
// import static software.wings.dl.PageRequest.Builder.aPageRequest;
// import static software.wings.utils.WingsTestConstants.APP_ID;
// import static software.wings.utils.WingsTestConstants.RELEASE_ID;
// import static software.wings.utils.WingsTestConstants.SERVICE_ID;
//
// import com.google.common.collect.Lists;
// import com.google.common.io.Files;
//
// import org.junit.Before;
// import org.junit.Test;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import software.wings.WingsBaseTest;
// import software.wings.beans.artifact.Artifact;
// import software.wings.beans.artifact.Artifact.Builder;
// import software.wings.beans.artifact.Artifact.Status;
// import software.wings.beans.artifact.ArtifactFile;
// import software.wings.dl.PageRequest;
// import software.wings.exception.WingsException;
// import software.wings.service.intfc.ArtifactService;
// import software.wings.service.intfc.FileService;
// import software.wings.service.intfc.FileService.FileBucket;
//
// import java.io.File;
// import javax.inject.Inject;
// import javax.validation.ConstraintViolationException;
// import javax.ws.rs.core.MultivaluedHashMap;
// import javax.ws.rs.core.UriInfo;
//
//
//
///**
// * Created by peeyushaggarwal on 4/4/16.
// */
// public class ArtifactServiceTest extends WingsBaseTest {
//
//
//  @Mock private FileService fileService;
//
//  @InjectMocks
//  @Inject
//  private ArtifactService artifactService;
//
//  private Builder builder =
//      anArtifact().withAppId(APP_ID).withRelease(aRelease().withUuid(RELEASE_ID).build()).withArtifactSourceName("ARTIFACT_SOURCE").withRevision("1.0")
//          .withDisplayName("DISPLAY_NAME").withCreatedAt(System.currentTimeMillis()).withCreatedBy(anEmbeddedUser().withUuid("USER_ID").build())
//          .withServices(Lists.newArrayList(aService().withAppId(APP_ID).withUuid(SERVICE_ID).build()));
//
//  /**
//   * test setup.
//   */
//  @Before
//  public void setUp() {
//    wingsRule.getDatastore().save(anApplication().withUuid(APP_ID).build());
//    wingsRule.getDatastore().save(aService().withAppId(APP_ID).withUuid(SERVICE_ID).build());
//    wingsRule.getDatastore().save(
//        aRelease().withUuid(RELEASE_ID).withAppId(APP_ID).withServices(asSet(aService().withAppId(APP_ID).withUuid(SERVICE_ID).build())).withArtifactSources(
//            Lists.newArrayList(aJenkinsArtifactSource().withSourceName("ARTIFACT_SOURCE").withArtifactPathServices(asList(
//                anArtifactPathServiceEntry().withArtifactPathRegex("*").withServices(asList(aService().withAppId(APP_ID).withUuid(SERVICE_ID).build()))
//                    .build())).build())).build());
//  }
//
//  /**
//   * Should create artifact when valid.
//   */
//  @Test
//  public void shouldCreateArtifactWhenValid() {
//    assertThat(artifactService.create(builder.but().build())).isNotNull();
//  }
//
//  /**
//   * Should throw exception when app id does not match for artifac to be created.
//   */
//  @Test
//  public void shouldThrowExceptionWhenAppIdDoesNotMatchForArtifacToBeCreated() {
//    assertThatExceptionOfType(WingsException.class).isThrownBy(() ->
//    artifactService.create(builder.but().withAppId("BAD_APP_ID").build()));
//  }
//
//  /**
//   * Should throw exception when release id does not match for artifac to be created.
//   */
//  @Test
//  public void shouldThrowExceptionWhenReleaseIdDoesNotMatchForArtifacToBeCreated() {
//    assertThatExceptionOfType(WingsException.class)
//        .isThrownBy(() ->
//        artifactService.create(builder.but().withRelease(aRelease().withUuid("RELEASE_ID1").build()).build()));
//  }
//
//  /**
//   * Should throw exception when artifact to be created is invalid.
//   */
//  @Test
//  public void shouldThrowExceptionWhenArtifactToBeCreatedIsInvalid() {
//    assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() ->
//    artifactService.create(builder.but().withRevision(null).build()));
//  }
//
//  /**
//   * Should update artifact when valid.
//   */
//  @Test
//  public void shouldUpdateArtifactWhenValid() {
//    Artifact savedArtifact = artifactService.create(builder.but().build());
//
//
//    savedArtifact.setDisplayName("ARTIFACT_DISPLAY_NAME");
//    assertThat(artifactService.update(savedArtifact)).isEqualTo(savedArtifact);
//  }
//
//  /**
//   * Should throw exception when artifact to be updated is invalid.
//   */
//  @Test
//  public void shouldThrowExceptionWhenArtifactToBeUpdatedIsInvalid() {
//    Artifact savedArtifact = artifactService.create(builder.but().build());
//
//    savedArtifact.setDisplayName(null);
//    assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() ->
//    artifactService.create(savedArtifact));
//  }
//
//  /**
//   * Should not download file for artifact when not ready.
//   */
//  @Test
//  public void shouldNotDownloadFileForArtifactWhenNotReady() {
//    Artifact savedArtifact = artifactService.create(builder.but().build());
//    assertThat(artifactService.download(APP_ID, savedArtifact.getUuid(), SERVICE_ID)).isNull();
//  }
//
//  /**
//   * Should download file for artifact when ready.
//   */
//  @Test
//  public void shouldDownloadFileForArtifactWhenReady() {
//    File file = null;
//    try {
//      Artifact savedArtifact = artifactService.create(builder.but().build());
//      ArtifactFile artifactFile =
//      anArtifactFile().withAppId(APP_ID).withName("test-artifact.war").withUuid("TEST_FILE_ID").build();
//      wingsRule.getDatastore().save(artifactFile);
//      savedArtifact.setArtifactFiles(Lists.newArrayList(artifactFile));
//      savedArtifact.setStatus(Status.READY);
//      wingsRule.getDatastore().save(savedArtifact);
//      when(fileService.download(anyString(), any(File.class), any(FileBucket.class))).thenAnswer(invocation -> {
//        File inputFile = invocation.getArgumentAt(1, File.class);
//        Files.write("Dummy".getBytes(), inputFile);
//        return inputFile;
//      });
//
//      file = artifactService.download(APP_ID, savedArtifact.getUuid(), SERVICE_ID);
//      assertThat(file).isNotNull().hasContent("Dummy");
//    } finally {
//      if (file != null) {
//        file.delete();
//      }
//    }
//  }
//
//  /**
//   * Should list artifact.
//   */
//  @Test
//  public void shouldListArtifact() {
//    Artifact savedArtifact = artifactService.create(builder.but().build());
//    assertThat(artifactService.list(new PageRequest<>())).hasSize(1).containsExactly(savedArtifact);
//  }
//
//  /**
//   * Should list artifact.
//   */
//  @Test
//  public void shouldListArtifactByReleases() {
//    wingsRule.getDatastore().save(aRelease().withUuid(RELEASE_ID + "1").withAppId(APP_ID)
//        .withArtifactSources(Lists.newArrayList(aJenkinsArtifactSource().withSourceName("ARTIFACT_SOURCE").build())).build());
//
//    Artifact savedArtifact1 = artifactService.create(builder.but().build());
//
//    Artifact savedArtifact2 = artifactService.create(builder.but().withRelease(aRelease().withUuid(RELEASE_ID +
//    "1").build()).build());
//
//    UriInfo uriInfo = mock(UriInfo.class);
//    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<String, String>() {{
//      putSingle("release", RELEASE_ID);
//    }});
//    assertThat(artifactService.list(aPageRequest().withUriInfo(uriInfo).build())).hasSize(1).containsExactly(savedArtifact1);
//
//    uriInfo = mock(UriInfo.class);
//    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<String, String>() {{
//      putSingle("release", RELEASE_ID + "1");
//    }});
//    assertThat(artifactService.list(aPageRequest().withUriInfo(uriInfo).build())).hasSize(1).containsExactly(savedArtifact2);
//  }
//
//  /**
//   * Should get artifact.
//   */
//  @Test
//  public void shouldGetArtifact() {
//    Artifact savedArtifact = artifactService.create(builder.but().build());
//    assertThat(artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid())).isEqualTo(savedArtifact);
//  }
//
//  /**
//   * Should soft delete artifact.
//   */
//  @Test
//  public void shouldDeleteArtifact() {
//    Artifact savedArtifact = artifactService.create(builder.but().build());
//    artifactService.delete(savedArtifact.getAppId(), savedArtifact.getUuid());
//    assertThat(artifactService.list(new PageRequest<>())).hasSize(0);
//  }
//}
