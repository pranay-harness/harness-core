package software.wings.service.impl.artifact;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.Artifact.ContentStatus.DELETED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADING;
import static software.wings.beans.artifact.Artifact.ContentStatus.FAILED;
import static software.wings.beans.artifact.Artifact.ContentStatus.METADATA_ONLY;
import static software.wings.beans.artifact.Artifact.ContentStatus.NOT_DOWNLOADED;
import static software.wings.beans.artifact.Artifact.Status;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;

import de.danielbechler.util.Collections;
import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.persistence.HQuery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Service;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Builder;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.ArtifactFile;
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
import software.wings.dl.WingsPersistence;
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
  @Mock private HQuery<Application> appQuery;
  @Mock private Queue<CollectEvent> collectQueue;
  @Mock private ServiceResourceService serviceResourceService;

  @InjectMocks @Inject private ArtifactService artifactService;

  private Builder artifactBuilder = anArtifact()
                                        .withAppId(APP_ID)
                                        .withMetadata(ImmutableMap.of("buildNo", "200"))
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
        Service.builder().appId(APP_ID).artifactType(ArtifactType.WAR).uuid(SERVICE_ID).build());
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
  public void shouldAddArtifactFile() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(savedArtifact).isNotNull().hasFieldOrPropertyWithValue("artifactSourceName", "ARTIFACT_SOURCE");
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    artifactService.addArtifactFile(savedArtifact.getUuid(), savedArtifact.getAppId(), asList(artifactFile));
    Artifact updatedArtifact = artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(updatedArtifact).isNotNull().extracting(Artifact::getArtifactFiles).hasSize(1);

    assertThat(updatedArtifact.getArtifactFiles().get(0).getFileUuid()).isEqualTo("5942bffe1e204f7f3004f455");
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
  public void shouldUpdateStatusApproved() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    artifactService.updateStatus(savedArtifact.getUuid(), savedArtifact.getAppId(), APPROVED);
    Artifact updatedArtifact = artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(updatedArtifact).isNotNull();
    assertThat(updatedArtifact.getStatus()).isEqualTo(APPROVED);
  }

  @Test
  public void shouldUpdateStatusWithErrorMessage() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    artifactService.updateStatus(
        savedArtifact.getUuid(), savedArtifact.getAppId(), Status.FAILED, "Failed to download artifact");
    Artifact updatedArtifact = artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(updatedArtifact).isNotNull();
    assertThat(updatedArtifact.getStatus()).isEqualTo(Status.FAILED);
    assertThat(updatedArtifact.getErrorMessage()).isEqualTo("Failed to download artifact");
  }

  @Test
  public void shouldUpdateContentStatus() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    artifactService.updateStatus(savedArtifact.getUuid(), savedArtifact.getAppId(), APPROVED, DOWNLOADED);
    Artifact updatedArtifact = artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(updatedArtifact).isNotNull();
    assertThat(updatedArtifact.getStatus()).isEqualTo(APPROVED);
    assertThat(updatedArtifact.getContentStatus()).isEqualTo(DOWNLOADED);
  }

  @Test
  public void shouldUpdateContentStatusWithErrorMessage() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    artifactService.updateStatus(
        savedArtifact.getUuid(), savedArtifact.getAppId(), Status.FAILED, FAILED, "Failed to download artifact");
    Artifact updatedArtifact = artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(updatedArtifact).isNotNull();
    assertThat(updatedArtifact.getStatus()).isEqualTo(Status.FAILED);
    assertThat(updatedArtifact.getContentStatus()).isEqualTo(FAILED);
    assertThat(updatedArtifact.getErrorMessage()).isEqualTo("Failed to download artifact");
  }

  @Test
  public void shouldNotDownloadFileForArtifactWhenNotReady() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(artifactService.download(APP_ID, savedArtifact.getUuid())).isNull();
  }

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

  @Test
  public void shouldListArtifact() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(artifactService.list(aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build(), false))
        .hasSize(1)
        .containsExactly(savedArtifact);
  }

  @Test
  public void shouldListArtifactWithDetails() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().appId(APP_ID).artifactType(ArtifactType.WAR).uuid(SERVICE_ID).build());
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    PageResponse<Artifact> artifacts =
        artifactService.list(aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build(), true);
    assertThat(artifacts).hasSize(1).extracting(Artifact::getUuid).contains(savedArtifact.getUuid());
    assertThat(artifacts).extracting(Artifact::getServices).hasSize(1);
  }

  @Test
  public void shouldListSortByBuildNo() {
    constructArtifacts();

    when(artifactStreamService.fetchArtifactStreamIdsForService(APP_ID, SERVICE_ID))
        .thenReturn(asList(ARTIFACT_STREAM_ID));
    List<Artifact> artifacts = artifactService.listSortByBuildNo(
        APP_ID, SERVICE_ID, aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build());

    assertThat(artifacts)
        .hasSize(4)
        .extracting(Artifact::getBuildNo)
        .containsSequence("todolist-1.0-15.x86_64.rpm", "todolist-1.0-10.x86_64.rpm", "todolist-1.0-5.x86_64.rpm",
            "todolist-1.0-1.x86_64.rpm");
  }

  @Test
  public void shouldListSortByBuildNoWithNoServiceId() {
    constructArtifacts();

    List<Artifact> artifacts = artifactService.listSortByBuildNo(
        APP_ID, null, aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build());

    assertThat(artifacts)
        .hasSize(4)
        .extracting(Artifact::getBuildNo)
        .containsSequence("todolist-1.0-15.x86_64.rpm", "todolist-1.0-10.x86_64.rpm", "todolist-1.0-5.x86_64.rpm",
            "todolist-1.0-1.x86_64.rpm");
  }

  @Test
  public void shouldNotListArtifactsOfDeletedArtifactStreams() {
    constructArtifacts();

    List<Artifact> artifacts = artifactService.listSortByBuildNo(
        APP_ID, SERVICE_ID, aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build());

    assertThat(artifacts).isEmpty();
  }

  private void constructArtifacts() {
    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-1.x86_64.rpm")).but().build());

    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-10.x86_64.rpm")).but().build());
    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-5.x86_64.rpm")).but().build());

    artifactService.create(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-15.x86_64.rpm")).but().build());
  }

  @Test
  public void shouldGetArtifact() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid())).isEqualTo(savedArtifact);
  }

  @Test
  public void shouldGetArtifactWithServices() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().appId(APP_ID).artifactType(ArtifactType.WAR).uuid(SERVICE_ID).build());
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    Artifact updatedArtifact = artifactService.get(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(updatedArtifact).isEqualTo(savedArtifact);
    assertThat(updatedArtifact).extracting(Artifact::getServices).hasSize(1);
  }

  @Test
  public void shouldFetchLatestArtifactForArtifactStream() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    Artifact latestArtifact = artifactService.fetchLatestArtifactForArtifactStream(
        savedArtifact.getAppId(), savedArtifact.getArtifactStreamId(), savedArtifact.getArtifactSourceName());
    assertThat(latestArtifact)
        .isNotNull()
        .extracting(Artifact::getArtifactSourceName)
        .containsExactly(savedArtifact.getArtifactSourceName());
  }

  @Test
  public void shouldFetchLastCollectedApprovedArtifactForArtifactStream() {
    Artifact artifact = artifactBuilder.build();
    Artifact savedArtifact = artifactService.create(artifact);
    artifactService.updateStatus(savedArtifact.getUuid(), savedArtifact.getAppId(), APPROVED);
    Artifact latestArtifact = artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
        savedArtifact.getAppId(), savedArtifact.getArtifactStreamId(), savedArtifact.getArtifactSourceName());
    assertThat(latestArtifact)
        .isNotNull()
        .extracting(Artifact::getArtifactSourceName)
        .containsExactly(savedArtifact.getArtifactSourceName());
  }

  @Test
  public void shouldFetchLastCollectedArtifactForArtifactStream() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.build());
    artifactService.updateStatus(savedArtifact.getUuid(), savedArtifact.getAppId(), RUNNING);
    Artifact latestArtifact = artifactService.fetchLastCollectedArtifact(
        savedArtifact.getAppId(), savedArtifact.getArtifactStreamId(), savedArtifact.getArtifactSourceName());
    assertThat(latestArtifact)
        .isNotNull()
        .extracting(Artifact::getArtifactSourceName)
        .containsExactly(savedArtifact.getArtifactSourceName());
  }

  @Test
  public void shouldGetArtifactByBuildNumber() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.build());
    artifactService.updateStatus(savedArtifact.getUuid(), savedArtifact.getAppId(), APPROVED);
    Artifact latestArtifact = artifactService.getArtifactByBuildNumber(
        savedArtifact.getAppId(), savedArtifact.getArtifactStreamId(), savedArtifact.getBuildNo());
    assertThat(latestArtifact)
        .isNotNull()
        .extracting(Artifact::getArtifactSourceName)
        .containsExactly(savedArtifact.getArtifactSourceName());
  }

  @Test
  public void shouldGetArtifactByBuildNumberSource() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.build());
    artifactService.updateStatus(savedArtifact.getUuid(), savedArtifact.getAppId(), APPROVED);
    Artifact latestArtifact = artifactService.getArtifactByBuildNumber(savedArtifact.getAppId(),
        savedArtifact.getArtifactStreamId(), savedArtifact.getArtifactSourceName(), savedArtifact.getBuildNo(), false);
    assertThat(latestArtifact)
        .isNotNull()
        .extracting(Artifact::getArtifactSourceName)
        .containsExactly(savedArtifact.getArtifactSourceName());
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
  public void shouldGetArtifactContentStatusDeleted() {
    Artifact artifact = artifactBuilder.withStatus(APPROVED).build();
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    when(artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId())).thenReturn(null);
    ContentStatus contentStatus = artifactService.getArtifactContentStatus(artifact);
    assertThat(contentStatus).isEqualTo(DELETED);
  }

  @Test
  public void shouldGetArtifactContentStatusStreamDeleted() {
    Artifact artifact = artifactBuilder.withUuid(ARTIFACT_ID).withStatus(APPROVED).build();
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    when(artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId())).thenReturn(null);
    ContentStatus contentStatus = artifactService.getArtifactContentStatus(artifact);
    assertThat(contentStatus).isEqualTo(METADATA_ONLY);
  }

  @Test
  public void shouldGetArtifactContentStatusStreamDeletedWithFiles() {
    Artifact artifact = artifactBuilder.withUuid(ARTIFACT_ID)
                            .withStatus(APPROVED)
                            .withArtifactFiles(asList(anArtifactFile().build()))
                            .build();
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    when(artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId())).thenReturn(null);
    ContentStatus contentStatus = artifactService.getArtifactContentStatus(artifact);
    assertThat(contentStatus).isEqualTo(DOWNLOADED);
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
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(ArtifactType.DOCKER).build());
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
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(ArtifactType.WAR).build());
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
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(ArtifactType.DOCKER).build());
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
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(ArtifactType.RPM).build());
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

  @Test
  public void shouldDeleteArtifact() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(wingsPersistence.get(Artifact.class, savedArtifact.getUuid())).isNotNull();
    artifactService.delete(savedArtifact.getAppId(), savedArtifact.getUuid());
    assertThat(wingsPersistence.get(Artifact.class, savedArtifact.getUuid())).isNull();
  }

  @Test
  public void shouldNotDeleteMetadataOnlyArtifacts() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    String dockerArtifactStreamId = wingsRule.getDatastore().save(dockerArtifactStream).getId().toString();

    JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                      .sourceName("todolistwar")
                                                      .settingId(SETTING_ID)
                                                      .appId(APP_ID)
                                                      .jobname("todolistwar")
                                                      .autoPopulate(true)
                                                      .metadataOnly(true)
                                                      .serviceId(SERVICE_ID)
                                                      .artifactPaths(asList("target/todolist.war"))
                                                      .build();

    String jenkinsArtifactStreamId = wingsRule.getDatastore().save(jenkinsArtifactStream).getId().toString();

    Artifact dockerArtifact = constructArtifact(dockerArtifactStreamId);

    Artifact jenkinsArtifact = constructArtifact(jenkinsArtifactStreamId);

    when(artifactStreamService.get(APP_ID, dockerArtifactStreamId)).thenReturn(dockerArtifactStream);
    artifactService.create(dockerArtifact);

    when(artifactStreamService.get(APP_ID, jenkinsArtifactStreamId)).thenReturn(jenkinsArtifactStream);
    artifactService.create(jenkinsArtifact);

    artifactService.deleteArtifacts(50);
    assertThat(artifactService.list(aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build(), false))
        .hasSize(2);
  }

  private Artifact constructArtifact(String dockerArtifactStreamId) {
    return anArtifact()
        .withAppId(APP_ID)
        .withArtifactStreamId(dockerArtifactStreamId)
        .withMetadata(ImmutableMap.of("buildNo", "200"))
        .withRevision("1.0")
        .withDisplayName("DISPLAY_NAME")
        .withServiceIds(asList(SERVICE_ID))
        .build();
  }

  @Test
  public void shouldDeleteArtifactsWithNoArtifactFiles() {
    JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                      .sourceName("todolistwar")
                                                      .settingId(SETTING_ID)
                                                      .appId(APP_ID)
                                                      .jobname("todolistwar")
                                                      .autoPopulate(true)
                                                      .serviceId(SERVICE_ID)
                                                      .artifactPaths(asList("target/todolist.war"))
                                                      .build();
    String jenkinsArtifactStreamId = wingsRule.getDatastore().save(jenkinsArtifactStream).getId().toString();
    Artifact jenkinsArtifact = anArtifact()
                                   .withAppId(APP_ID)
                                   .withArtifactStreamId(jenkinsArtifactStreamId)
                                   .withMetadata(ImmutableMap.of("buildNo", "200"))
                                   .withRevision("1.0")
                                   .withDisplayName("DISPLAY_NAME")
                                   .withServiceIds(asList(SERVICE_ID))
                                   .withStatus(APPROVED)
                                   .withContentStatus(DOWNLOADED)
                                   .build();

    when(artifactStreamService.get(APP_ID, jenkinsArtifactStreamId)).thenReturn(jenkinsArtifactStream);
    Artifact savedArtifact = artifactService.create(jenkinsArtifact);

    wingsRule.getDatastore().save(savedArtifact);
    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build(), false))
        .hasSize(1);
  }

  @Test
  public void shouldDeleteArtifactsWithArtifactFiles() {
    JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                      .sourceName("todolistwar")
                                                      .settingId(SETTING_ID)
                                                      .appId(APP_ID)
                                                      .jobname("todolistwar")
                                                      .autoPopulate(true)
                                                      .serviceId(SERVICE_ID)
                                                      .artifactPaths(asList("target/todolist.war"))
                                                      .build();
    String jenkinsArtifactStreamId = wingsRule.getDatastore().save(jenkinsArtifactStream).getId().toString();
    Artifact jenkinsArtifact = anArtifact()
                                   .withAppId(APP_ID)
                                   .withArtifactStreamId(jenkinsArtifactStreamId)
                                   .withMetadata(ImmutableMap.of("buildNo", "200"))
                                   .withRevision("1.0")
                                   .withDisplayName("DISPLAY_NAME")
                                   .withServiceIds(asList(SERVICE_ID))
                                   .withStatus(APPROVED)
                                   .build();

    when(artifactStreamService.get(APP_ID, jenkinsArtifactStreamId)).thenReturn(jenkinsArtifactStream);
    Artifact savedArtifact = artifactService.create(jenkinsArtifact);
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    wingsRule.getDatastore().save(artifactFile);
    savedArtifact.setArtifactFiles(asList(artifactFile));
    savedArtifact.setContentStatus(DOWNLOADED);

    wingsRule.getDatastore().save(savedArtifact);

    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build(), false))
        .hasSize(0);
  }

  @Test
  public void shouldNotDeleteFailedArtifacts() {
    JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                      .sourceName("todolistwar")
                                                      .settingId(SETTING_ID)
                                                      .appId(APP_ID)
                                                      .jobname("todolistwar")
                                                      .autoPopulate(true)
                                                      .serviceId(SERVICE_ID)
                                                      .artifactPaths(asList("target/todolist.war"))
                                                      .build();
    String jenkinsArtifactStreamId = wingsRule.getDatastore().save(jenkinsArtifactStream).getId().toString();
    Artifact jenkinsArtifact = anArtifact()
                                   .withAppId(APP_ID)
                                   .withArtifactStreamId(jenkinsArtifactStreamId)
                                   .withMetadata(ImmutableMap.of("buildNo", "200"))
                                   .withRevision("1.0")
                                   .withDisplayName("DISPLAY_NAME")
                                   .withServiceIds(asList(SERVICE_ID))
                                   .withStatus(APPROVED)
                                   .build();

    when(artifactStreamService.get(APP_ID, jenkinsArtifactStreamId)).thenReturn(jenkinsArtifactStream);
    Artifact savedArtifact = artifactService.create(jenkinsArtifact);
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    wingsRule.getDatastore().save(artifactFile);
    savedArtifact.setArtifactFiles(asList(artifactFile));
    savedArtifact.setContentStatus(FAILED);

    wingsRule.getDatastore().save(savedArtifact);

    artifactService.deleteArtifacts(0);
    assertThat(artifactService.list(aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, APP_ID).build(), false))
        .hasSize(1);
  }

  @Test
  public void shouldNotDeleteArtifactsGreaterThanRetentionTime() {
    JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                      .sourceName("todolistwar")
                                                      .settingId(SETTING_ID)
                                                      .appId(APP_ID)
                                                      .jobname("todolistwar")
                                                      .autoPopulate(true)
                                                      .serviceId(SERVICE_ID)
                                                      .artifactPaths(asList("target/todolist.war"))
                                                      .build();
    String jenkinsArtifactStreamId = wingsRule.getDatastore().save(jenkinsArtifactStream).getId().toString();
    Artifact jenkinsArtifact = anArtifact()
                                   .withAppId(APP_ID)
                                   .withArtifactStreamId(jenkinsArtifactStreamId)
                                   .withMetadata(ImmutableMap.of("buildNo", "200"))
                                   .withRevision("1.0")
                                   .withDisplayName("DISPLAY_NAME")
                                   .withServiceIds(asList(SERVICE_ID))
                                   .withStatus(APPROVED)
                                   .build();

    when(artifactStreamService.get(APP_ID, jenkinsArtifactStreamId)).thenReturn(jenkinsArtifactStream);
    Artifact savedArtifact = artifactService.create(jenkinsArtifact);
    ArtifactFile artifactFile = anArtifactFile()
                                    .withAppId(APP_ID)
                                    .withName("test-artifact.war")
                                    .withUuid("5942bffe1e204f7f3004f455")
                                    .withFileUuid("5942bffe1e204f7f3004f455")
                                    .build();
    wingsRule.getDatastore().save(artifactFile);
    savedArtifact.setArtifactFiles(asList(artifactFile));
    savedArtifact.setContentStatus(DOWNLOADED);

    wingsRule.getDatastore().save(savedArtifact);

    artifactService.deleteArtifacts(1);
    assertThat(artifactService.list(
                   aPageRequest().addFilter(Artifact.APP_ID_KEY, EQ, savedArtifact.getAppId()).build(), false))
        .hasSize(1);
  }

  @Test
  public void shouldFetchArtifacts() {
    Artifact savedArtifact = artifactService.create(artifactBuilder.but().build());
    assertThat(wingsPersistence.get(Artifact.class, savedArtifact.getUuid())).isNotNull();
    assertThat(artifactService.fetchArtifacts(APP_ID, Collections.setOf(asList(savedArtifact.getUuid())))).isNotEmpty();
  }
}
