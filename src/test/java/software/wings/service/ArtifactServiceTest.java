package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.beans.Service.ServiceBuilder.aService;
import static software.wings.beans.User.Builder.anUser;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Builder;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.dl.PageRequest;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class ArtifactServiceTest extends WingsBaseTest {
  public static final String APP_ID = "APP_ID";
  public static final String RELEASE_ID = "RELEASE_ID";
  public static final String SERVICE_ID = "SERVICE_ID";
  @Mock private FileService fileService;

  @InjectMocks @Inject private ArtifactService artifactService;

  private Builder builder = anArtifact()
                                .withAppId(APP_ID)
                                .withRelease(aRelease().withUuid(RELEASE_ID).build())
                                .withArtifactSourceName("ARTIFACT_SOURCE")
                                .withRevision("1.0")
                                .withDisplayName("DISPLAY_NAME")
                                .withCreatedAt(System.currentTimeMillis())
                                .withCreatedBy(anUser().withUuid("USER_ID").build());

  /**
   * test setup.
   */
  @Before
  public void setUp() {
    wingsRule.getDatastore().save(anApplication().withUuid(APP_ID).build());
    wingsRule.getDatastore().save(
        aRelease()
            .withUuid(RELEASE_ID)
            .withAppId(APP_ID)
            .withArtifactSources(Lists.newArrayList(aJenkinsArtifactSource().withSourceName("ARTIFACT_SOURCE").build()))
            .build());
    wingsRule.getDatastore().save(aService().withAppId(APP_ID).withUuid(SERVICE_ID).build());
  }

  @Test
  public void shouldCreateArtifactWhenValid() {
    assertThat(artifactService.create(builder.but().build())).isNotNull();
  }

  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> artifactService.create(builder.but().withAppId("BAD_APP_ID").build()));
  }

  @Test
  public void shouldThrowExceptionWhenReleaseIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(()
                        -> artifactService.create(
                            builder.but().withRelease(aRelease().withUuid("RELEASE_ID1").build()).build()));
  }

  @Test
  public void shouldThrowExceptionWhenArtifactToBeCreatedIsInvalid() {
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(builder.but().withRevision(null).build()));
  }

  @Test
  public void shouldUpdateArtifactWhenValid() {
    Artifact savedArtifact = artifactService.create(builder.but().build());

    savedArtifact.setDisplayName("ARTIFACT_DISPLAY_NAME");
    assertThat(artifactService.update(savedArtifact)).isEqualTo(savedArtifact);
  }

  @Test
  public void shouldThrowExceptionWhenArtifactToBeUpdatedIsInvalid() {
    Artifact savedArtifact = artifactService.create(builder.but().build());

    savedArtifact.setDisplayName(null);
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(savedArtifact));
  }

  @Test
  public void shouldNotDownloadFileForArtifactWhenNotReady() {
    Artifact savedArtifact = artifactService.create(builder.but().build());
    assertThat(artifactService.download(APP_ID, savedArtifact.getUuid(), SERVICE_ID)).isNull();
  }

  @Test
  public void shouldDownloadFileForArtifactWhenReady() {
    File file = null;
    try {
      Artifact savedArtifact = artifactService.create(builder.but().build());
      ArtifactFile artifactFile =
          anArtifactFile()
              .withAppId(APP_ID)
              .withName("test-artifact.war")
              .withUuid("TEST_FILE_ID")
              .withServices(Lists.newArrayList(aService().withAppId(APP_ID).withUuid(SERVICE_ID).build()))
              .build();
      wingsRule.getDatastore().save(artifactFile);
      savedArtifact.setArtifactFiles(Lists.newArrayList(artifactFile));
      savedArtifact.setStatus(Status.READY);
      wingsRule.getDatastore().save(savedArtifact);
      when(fileService.download(anyString(), any(File.class), any(FileBucket.class))).thenAnswer(invocation -> {
        File inputFile = invocation.getArgumentAt(1, File.class);
        Files.write("Dummy".getBytes(), inputFile);
        return inputFile;
      });

      file = artifactService.download(APP_ID, savedArtifact.getUuid(), SERVICE_ID);
      assertThat(file).isNotNull().hasContent("Dummy");
    } finally {
      if (file != null) {
        file.delete();
      }
    }
  }

  @Test
  public void shouldListArtifact() {
    Artifact savedArtifact = artifactService.create(builder.but().build());
    assertThat(artifactService.list(new PageRequest<>())).hasSize(1).containsExactly(savedArtifact);
  }

  @Test
  public void shouldGetArtifact() {
    Artifact savedArtifact = artifactService.create(builder.but().build());
    assertThat(artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid())).isEqualTo(savedArtifact);
  }

  @Test
  public void shouldSoftDeleteArtifact() {
    Artifact savedArtifact = artifactService.create(builder.but().build());
    artifactService.softDelete(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(artifactService.list(new PageRequest<>())).hasSize(0);
  }
}
