package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Builder;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.ArtifactType;

import java.io.File;
import javax.validation.ConstraintViolationException;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
@SetupScheduler
public class ArtifactServiceTest extends WingsBaseTest {
  @Inject @Spy private WingsPersistence wingsPersistence;

  @Mock private FileService fileService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private AppService appService;
  @Mock private Query<Application> appQuery;
  @Mock private Query<ArtifactStream> artifactStreamQuery;
  @Mock private FieldEnd fieldEnd;

  @InjectMocks @Inject private ArtifactService artifactService;

  private Builder artifactBuilder = anArtifact()
                                        .withAppId(APP_ID)
                                        .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                        .withRevision("1.0")
                                        .withDisplayName("DISPLAY_NAME")
                                        .withCreatedAt(System.currentTimeMillis())
                                        .withCreatedBy(EmbeddedUser.builder().uuid("USER_ID").build())
                                        .withServiceIds(asList(SERVICE_ID));

  JenkinsArtifactStream jenkinsArtifactStream = aJenkinsArtifactStream()
                                                    .withUuid(ARTIFACT_STREAM_ID)
                                                    .withAppId(APP_ID)
                                                    .withSourceName("ARTIFACT_SOURCE")
                                                    .withServiceId(SERVICE_ID)
                                                    .build();

  /**
   * test setup.
   */
  @Before
  public void setUp() {
    wingsRule.getDatastore().save(
        aService().withAppId(APP_ID).withArtifactType(ArtifactType.WAR).withUuid(SERVICE_ID).build());
    when(appQuery.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(appQuery);

    when(appService.exist(APP_ID)).thenReturn(true);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
  }

  /**
   * Should create artifact when valid.
   */
  @Test
  public void shouldCreateArtifactWhenValid() {
    assertThat(artifactService.create(artifactBuilder.but().build()))
        .isNotNull()
        .hasFieldOrPropertyWithValue("artifactSourceName", "ARTIFACT_SOURCE");
  }
  /**
   * Should throw exception when app id does not match for artifac to be created.
   */
  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> artifactService.create(artifactBuilder.but().withAppId("BAD_APP_ID").build()));
  }

  /**
   * Should throw exception when release id does not match for artifac to be created.
   */
  @Test
  public void shouldThrowExceptionWhenReleaseIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(
            () -> artifactService.create(artifactBuilder.but().withArtifactStreamId("NON_EXISTENT_ID").build()));
  }

  /**
   * Should throw exception when artifact to be created is invalid.
   */
  @Test
  @Ignore
  public void shouldThrowExceptionWhenArtifactToBeCreatedIsInvalid() {
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(artifactBuilder.but().withRevision(null).build()));
  }

  /**
   * Should update artifact when valid.
   */
  @Test
  public void shouldUpdateArtifactWhenValid() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());

    savedArtifact.setDisplayName("ARTIFACT_DISPLAY_NAME");
    assertThat(artifactService.update(savedArtifact)).isEqualTo(savedArtifact);
  }

  /**
   * Should throw exception when artifact to be updated is invalid.
   */
  @Test
  public void shouldThrowExceptionWhenArtifactToBeUpdatedIsInvalid() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());

    savedArtifact.setDisplayName(null);
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(savedArtifact));
  }

  /**
   * Should not download file for artifact when not ready.
   */
  @Test
  public void shouldNotDownloadFileForArtifactWhenNotReady() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(artifactService.download(APP_ID, savedArtifact.getUuid())).isNull();
  }

  /**
   * Should download file for artifact when ready.
   */
  @Test
  public void shouldDownloadFileForArtifactWhenReady() {
    File file = null;
    try {
      Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
      ArtifactFile artifactFile =
          anArtifactFile().withAppId(APP_ID).withName("test-artifact.war").withUuid("TEST_FILE_ID").build();
      wingsRule.getDatastore().save(artifactFile);
      savedArtifact.setArtifactFiles(Lists.newArrayList(artifactFile));
      savedArtifact.setStatus(Status.READY);
      wingsRule.getDatastore().save(savedArtifact);
      when(fileService.download(anyString(), any(File.class), any(FileBucket.class))).thenAnswer(invocation -> {
        File inputFile = invocation.getArgumentAt(1, File.class);
        Files.write("Dummy".getBytes(), inputFile);
        return inputFile;
      });

      file = artifactService.download(APP_ID, savedArtifact.getUuid());
      assertThat(file).isNotNull().hasContent("Dummy");
    } finally {
      if (file != null) {
        file.delete();
      }
    }
  }

  /**
   * Should list artifact.
   */
  @Test
  public void shouldListArtifact() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(1).containsExactly(savedArtifact);
  }

  /**
   * Should get artifact.
   */
  @Test
  public void shouldGetArtifact() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid())).isEqualTo(savedArtifact);
  }

  /**
   * Should soft delete artifact.
   */
  @Test
  public void shouldDeleteArtifact() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(wingsPersistence.get(Artifact.class, savedArtifact.getUuid())).isNotNull();
    artifactService.delete(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(wingsPersistence.get(Artifact.class, savedArtifact.getUuid())).isNull();
  }

  @Test
  public void shouldNotDeleteArtifacts() {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(appQuery);
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(artifactStreamQuery);
    when(appQuery.asKeyList()).thenReturn(asList(new Key(Application.class, "applications", APP_ID)));
    when(artifactStreamQuery.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(artifactStreamQuery);
    when(artifactStreamQuery.asList()).thenReturn(asList(jenkinsArtifactStream));

    artifactService.create(artifactBuilder.but().build());
    artifactService.deleteArtifacts(50);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(1);
  }

  @Test
  public void shouldDeleteArtifactsWithArtifactFiles() {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(appQuery);
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(artifactStreamQuery);
    when(appQuery.asKeyList()).thenReturn(asList(new Key(Application.class, "applications", APP_ID)));
    when(artifactStreamQuery.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(artifactStreamQuery);
    when(artifactStreamQuery.asList()).thenReturn(asList(jenkinsArtifactStream));

    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    wingsRule.getDatastore().save(artifactFile);
    savedArtifact.setArtifactFiles(Lists.newArrayList(artifactFile));
    savedArtifact.setStatus(Status.READY);
    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(0);
  }

  @Test
  public void shouldNotDeleteFailedArtifacts() {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(appQuery);
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(artifactStreamQuery);
    when(appQuery.asKeyList()).thenReturn(asList(new Key(Application.class, "applications", APP_ID)));
    when(artifactStreamQuery.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(artifactStreamQuery);
    when(artifactStreamQuery.asList()).thenReturn(asList(jenkinsArtifactStream));

    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    wingsRule.getDatastore().save(artifactFile);
    savedArtifact.setArtifactFiles(Lists.newArrayList(artifactFile));
    savedArtifact.setStatus(Status.FAILED);
    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(1);
  }

  @Test
  public void shouldNotDeleteArtifactsGreaterThanRetentionTime() {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(appQuery);
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(artifactStreamQuery);
    when(appQuery.asKeyList()).thenReturn(asList(new Key(Application.class, "applications", APP_ID)));
    when(artifactStreamQuery.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(artifactStreamQuery);
    when(artifactStreamQuery.asList()).thenReturn(asList(jenkinsArtifactStream));

    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    wingsRule.getDatastore().save(artifactFile);
    savedArtifact.setArtifactFiles(Lists.newArrayList(artifactFile));
    savedArtifact.setStatus(Status.READY);
    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(1);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(1);
  }
}
