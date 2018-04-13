package software.wings.service.impl.artifact;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADING;
import static software.wings.beans.artifact.Artifact.ContentStatus.METADATA_ONLY;
import static software.wings.beans.artifact.Artifact.ContentStatus.NOT_DOWNLOADED;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Builder;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.collect.CollectEvent;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

import java.io.File;
import java.util.List;
import javax.validation.ConstraintViolationException;

@SetupScheduler
public class ArtifactServiceTest extends WingsBaseTest {
  @Inject @Spy private WingsPersistence wingsPersistence;

  @Mock private FileService fileService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private AppService appService;
  @Mock private Query<Application> appQuery;
  @Mock private Query<ArtifactStream> artifactStreamQuery;
  @Mock private Queue<CollectEvent> collectQueue;
  @Mock private ServiceResourceService serviceResourceService;

  @InjectMocks @Inject private ArtifactService artifactService;

  private Builder artifactBuilder = anArtifact()
                                        .withAppId(APP_ID)
                                        .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                        .withRevision("1.0")
                                        .withDisplayName("DISPLAY_NAME")
                                        .withCreatedAt(System.currentTimeMillis())
                                        .withCreatedBy(EmbeddedUser.builder().uuid("USER_ID").build())
                                        .withServiceIds(asList(SERVICE_ID));

  private Artifact artifact = artifactBuilder.build();

  JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .build();

  @Before
  public void setUp() {
    wingsRule.getDatastore().save(
        aService().withAppId(APP_ID).withArtifactType(ArtifactType.WAR).withUuid(SERVICE_ID).build());
    when(appQuery.filter(anyString(), anyObject())).thenReturn(appQuery);

    when(appService.exist(APP_ID)).thenReturn(true);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
  }

  @Test
  public void shouldCreateArtifactWhenValid() {
    assertThat(artifactService.create(artifactBuilder.but().build()))
        .isNotNull()
        .hasFieldOrPropertyWithValue("artifactSourceName", "ARTIFACT_SOURCE");
  }

  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> artifactService.create(artifactBuilder.but().withAppId("BAD_APP_ID").build()));
  }

  @Test
  public void shouldThrowExceptionWhenReleaseIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(
            () -> artifactService.create(artifactBuilder.but().withArtifactStreamId("NON_EXISTENT_ID").build()));
  }

  @Test
  @Ignore
  public void shouldThrowExceptionWhenArtifactToBeCreatedIsInvalid() {
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(artifactBuilder.but().withRevision(null).build()));
  }

  @Test
  public void shouldUpdateArtifactWhenValid() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());

    savedArtifact.setDisplayName("ARTIFACT_DISPLAY_NAME");
    assertThat(artifactService.update(savedArtifact)).isEqualTo(savedArtifact);
  }

  @Test
  public void shouldThrowExceptionWhenArtifactToBeUpdatedIsInvalid() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());

    savedArtifact.setDisplayName(null);
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(savedArtifact));
  }

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
      savedArtifact.setStatus(READY);
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

  @Test
  public void shouldListSortByBuildNo() {
    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-1.x86_64.rpm")).but().build());

    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-10.x86_64.rpm")).but().build());
    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-5.x86_64.rpm")).but().build());

    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-15.x86_64.rpm")).but().build());

    List<Artifact> artifacts = artifactService.listSortByBuildNo(new PageRequest<>());

    assertThat(artifacts)
        .hasSize(4)
        .extracting(Artifact::getBuildNo)
        .containsSequence("todolist-1.0-15.x86_64.rpm", "todolist-1.0-10.x86_64.rpm", "todolist-1.0-5.x86_64.rpm",
            "todolist-1.0-1.x86_64.rpm");
  }

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
    when(artifactStreamQuery.filter(anyString(), anyObject())).thenReturn(artifactStreamQuery);
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
    when(artifactStreamQuery.filter(anyString(), anyObject())).thenReturn(artifactStreamQuery);
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
    savedArtifact.setStatus(READY);
    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(0);
  }

  @Test
  public void shouldNotDeleteFailedArtifacts() {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(appQuery);
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(artifactStreamQuery);
    when(appQuery.asKeyList()).thenReturn(asList(new Key(Application.class, "applications", APP_ID)));
    when(artifactStreamQuery.filter(anyString(), anyObject())).thenReturn(artifactStreamQuery);
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
    savedArtifact.setStatus(FAILED);
    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(1);
  }

  @Test
  public void shouldNotDeleteArtifactsGreaterThanRetentionTime() {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(appQuery);
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(artifactStreamQuery);
    when(appQuery.asKeyList()).thenReturn(asList(new Key(Application.class, "applications", APP_ID)));
    when(artifactStreamQuery.filter(anyString(), anyObject())).thenReturn(artifactStreamQuery);
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
    savedArtifact.setStatus(READY);
    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(1);
    assertThat(artifactService.list(new PageRequest<>(), false)).hasSize(1);
  }

  @Test(expected = WingsException.class)
  public void shouldStartArtifactCollectionNoArtifact() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(null);
    artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
  }

  @Test
  public void shouldNotCollectArtifactAlreadyQueued() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID))
        .thenReturn(artifactBuilder.withStatus(QUEUED).build());
    Artifact artifact = artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
    assertThat(artifact.getStatus()).isEqualTo(QUEUED);
    Mockito.verify(collectQueue, times(0)).send(any());
  }

  @Test
  public void shouldNotCollectArtifactAlreadyRunning() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID))
        .thenReturn(artifactBuilder.withStatus(RUNNING).build());
    Artifact artifact = artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
    assertThat(artifact.getStatus()).isEqualTo(RUNNING);
    Mockito.verify(collectQueue, times(0)).send(any());
  }

  @Test
  public void shouldNotCollectArtifactWhenContentStatusMetadata() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID))
        .thenReturn(artifactBuilder.withStatus(APPROVED).withContentStatus(METADATA_ONLY).build());
    Artifact artifact = artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
    Mockito.verify(collectQueue, times(0)).send(any());
  }

  @Test
  public void shouldNotCollectArtifactWhenContentStatusDOWNLOADING() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID))
        .thenReturn(artifactBuilder.withStatus(APPROVED).withContentStatus(DOWNLOADING).build());
    Artifact artifact = artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
    Mockito.verify(collectQueue, times(0)).send(any());
  }

  @Test
  public void shouldNotCollectArtifactWhenContentStatusDOWNLOADED() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID))
        .thenReturn(artifactBuilder.withStatus(APPROVED).withContentStatus(DOWNLOADED).build());
    Artifact artifact = artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
    assertThat(artifact.getContentStatus()).isEqualTo(DOWNLOADED);
    Mockito.verify(collectQueue, times(0)).send(any());
  }

  @Test
  public void shouldCollectArtifact() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID))
        .thenReturn(artifactBuilder.withStatus(APPROVED).withContentStatus(NOT_DOWNLOADED).build());
    Artifact artifact = artifactService.startArtifactCollection(APP_ID, ARTIFACT_ID);
    assertThat(artifact.getContentStatus()).isEqualTo(NOT_DOWNLOADED);
    Mockito.verify(collectQueue).send(any());
  }

  @Test
  public void shouldGetArtifactContentStatus() {
    Artifact artifact = artifactBuilder.withStatus(APPROVED).withContentStatus(NOT_DOWNLOADED).build();
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    ContentStatus contentStatus = artifactService.getArtifactContentStatus(artifact);
    assertThat(contentStatus).isEqualTo(NOT_DOWNLOADED);
  }

  @Test
  public void shouldGetArtifactStatusForDockerStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(DockerArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForEcrStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(EcrArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForGcrStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(GcrArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForAcrStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(GcrArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForAmiStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(AmiArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForS3Stream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(AmazonS3ArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForNexusDockerStream() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(aService().withUuid(SERVICE_ID).withArtifactType(ArtifactType.DOCKER).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(NexusArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForNexusStream() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(aService().withUuid(SERVICE_ID).withArtifactType(ArtifactType.WAR).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(NexusArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(NOT_DOWNLOADED);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForArtifactoryDockerStream() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(aService().withUuid(SERVICE_ID).withArtifactType(ArtifactType.DOCKER).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(ArtifactoryArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(METADATA_ONLY);
    assertThat(artifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldGetArtifactStatusForArtifactoryStream() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(aService().withUuid(SERVICE_ID).withArtifactType(ArtifactType.RPM).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(ArtifactoryArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(null);
    assertThat(artifact.getStatus()).isEqualTo(QUEUED);
  }

  @Test
  public void shouldGetArtifactStatusForJenkinsStream() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifactBuilder.build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(null);
    assertThat(artifact.getStatus()).isEqualTo(QUEUED);
  }

  @Test
  public void shouldGetArtifactStatusForBambooStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(BambooArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .appId(APP_ID)
                        .sourceName("ARTIFACT_SOURCE")
                        .serviceId(SERVICE_ID)
                        .build());
    assertThat(artifactService.getArtifactContentStatus(artifact)).isEqualTo(null);
    assertThat(artifact.getStatus()).isEqualTo(QUEUED);
  }
}
