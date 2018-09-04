package software.wings.service.impl.artifactstream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_USER_NAME_KEY;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.NexusConfig;
import software.wings.dl.PageRequest;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArtifactStreamServiceTest extends WingsBaseTest {
  @Mock private JobScheduler jobScheduler;
  @Mock private YamlPushService yamlPushService;
  @Mock private AppService appService;
  @Mock private BuildSourceService buildSourceService;
  @Mock private TriggerService triggerService;
  @Mock private SettingsService settingsService;
  @InjectMocks @Inject private ArtifactStreamService artifactStreamService;

  @Before
  public void setUp() {
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
  }
  @Test
  public void shouldAddJenkinsArtifactStream() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(jenkinsArtifactStream);

    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getAppId()).isNotEmpty();

    String artifactDisplayName = savedArtifactSteam.getArtifactDisplayName("40");
    assertThat(artifactDisplayName).isNotEmpty().contains("todolistwar");
    String[] values = artifactDisplayName.split("_");
    assertThat(values).hasSize(3);
    assertThat(values[0]).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam).isInstanceOf(JenkinsArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(JENKINS.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("todolistwar"));

    JenkinsArtifactStream savedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(savedJenkinsArtifactStream.getJobname()).isEqualTo("todolistwar");
    assertThat(savedJenkinsArtifactStream.getArtifactPaths().contains("*WAR"));

    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    verify(appService).getAccountIdByAppId(APP_ID);
  }

  private JenkinsArtifactStream getJenkinsStream() {
    return JenkinsArtifactStream.builder()
        .sourceName("todolistwar")
        .settingId(SETTING_ID)
        .appId(APP_ID)
        .jobname("todolistwar")
        .autoPopulate(true)
        .serviceId(SERVICE_ID)
        .artifactPaths(asList("target/todolist.war"))
        .build();
  }

  @Test
  public void shouldUpdateJenkinsArtifactStream() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(jenkinsArtifactStream);

    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);

    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("todolistwar");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam).isInstanceOf(JenkinsArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(JENKINS.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("todolistwar"));

    JenkinsArtifactStream savedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(savedJenkinsArtifactStream.getJobname()).isEqualTo("todolistwar");
    assertThat(savedJenkinsArtifactStream.getArtifactPaths().contains("target/todolist.war"));

    savedJenkinsArtifactStream.setName("JekinsName_Changed");
    savedJenkinsArtifactStream.setJobname("todoliswar_changed");
    savedJenkinsArtifactStream.setArtifactPaths(asList("*WAR_Changed"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedJenkinsArtifactStream);

    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("JekinsName_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);

    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("todoliswar_changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("todoliswar_changed");
    assertThat(updatedArtifactStream).isInstanceOf(JenkinsArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(JENKINS.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("todolistwar"));
    JenkinsArtifactStream updatedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(updatedJenkinsArtifactStream.getJobname()).isEqualTo("todoliswar_changed");
    assertThat(updatedJenkinsArtifactStream.getArtifactPaths().contains("*WAR_Changed"));

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  @Test
  public void shouldAddBambooArtifactStream() {
    ArtifactStream savedArtifactSteam = createBambooArtifactStream();
    BambooArtifactStream savedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths().contains("artifacts/todolist.war"));

    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    verify(appService).getAccountIdByAppId(APP_ID);
  }

  @Test
  public void shouldUpdateBambooArtifactStream() {
    ArtifactStream savedArtifactSteam = createBambooArtifactStream();

    BambooArtifactStream savedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths().contains("artifacts/todolist.war"));

    savedBambooArtifactStream.setName("Bamboo_Changed");
    savedBambooArtifactStream.setJobname("TOD-TOD_Changed");
    savedBambooArtifactStream.setArtifactPaths(asList("artifacts/todolist_changed.war"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedBambooArtifactStream);

    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Bamboo_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);

    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("TOD-TOD_Changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("TOD-TOD_Changed");
    assertThat(updatedArtifactStream).isInstanceOf(BambooArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(BAMBOO.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("TOD-TOD_Changed"));
    BambooArtifactStream updatedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(updatedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD_Changed");
    assertThat(updatedBambooArtifactStream.getArtifactPaths().contains("artifacts/todolist_changed.war"));

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  private ArtifactStream createBambooArtifactStream() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(bambooArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("TOD-TOD");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("TOD-TOD");
    assertThat(savedArtifactSteam).isInstanceOf(BambooArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(BAMBOO.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("TOD-TOD"));
    return savedArtifactSteam;
  }

  @Test
  public void shouldAddNexusArtifactStream() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStream();

    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("io.harness.test");
    assertThat(savedNexusArtifactStream.getArtifactPaths().contains("todolist"));
  }

  private ArtifactStream createNexusArtifactStream() {
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .groupId("io.harness.test")
                                                  .artifactPaths(asList("todolist"))
                                                  .autoPopulate(true)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("releases/io.harness.test/todolist__");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("releases/io.harness.test/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(NexusArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(NEXUS.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("releases"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId().equals("io.harness.test"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactName().equals("todolist"));
    return savedArtifactSteam;
  }

  @Test
  public void shouldUpdateNexusArtifactStream() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStream();
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getArtifactPaths().contains("todolist"));

    savedNexusArtifactStream.setName("Nexus_Changed");
    savedNexusArtifactStream.setJobname("snapshots");
    savedNexusArtifactStream.setGroupId("io.harness.test.changed");
    savedNexusArtifactStream.setArtifactPaths(asList("todolist-changed"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedNexusArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty();
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("snapshots/io.harness.test.changed/todolist-changed__");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("snapshots/io.harness.test.changed/todolist-changed");
    assertThat(updatedArtifactStream).isInstanceOf(NexusArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(NEXUS.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("snapshots"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getGroupId().equals("io.harness.test.changed"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactName().equals("todolist-changed"));
    NexusArtifactStream updatedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(updatedNexusArtifactStream.getJobname()).isEqualTo("snapshots");
    assertThat(updatedNexusArtifactStream.getArtifactPaths().contains("todolist-changed"));

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  @Test
  public void shouldAddNexusDockerArtifactStream() {
    NexusArtifactStream nexusDockerArtifactStream = NexusArtifactStream.builder()
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("docker-private")
                                                        .groupId("wingsplugings/todolist")
                                                        .imageName("wingsplugings/todolist")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(nexusDockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-private/wingsplugings/todolist__");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker-private/wingsplugings/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(NexusArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(NEXUS.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("docker-private"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId().equals("wingsplugings/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugings/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactName().isEmpty());
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("docker-private");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedNexusArtifactStream.getImageName()).isEqualTo("wingsplugings/todolist");
  }

  @Test
  public void shouldUpdateNexusDockerArtifactStream() {
    NexusArtifactStream nexusDockerArtifactStream = NexusArtifactStream.builder()
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("docker-private")
                                                        .groupId("wingsplugings/todolist")
                                                        .imageName("wingsplugings/todolist")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(nexusDockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-private/wingsplugings/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker-private/wingsplugings/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(NexusArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(NEXUS.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("docker-private"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId().equals("wingsplugings/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugings/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactName().isEmpty());
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("docker-private");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedNexusArtifactStream.getImageName()).isEqualTo("wingsplugings/todolist");

    savedNexusArtifactStream.setName("Nexus_Changed");
    savedNexusArtifactStream.setJobname("docker-hub");
    savedNexusArtifactStream.setGroupId("wingsplugings/todolist-changed");
    savedNexusArtifactStream.setImageName("wingsplugings/todolist-changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedNexusArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Nexus_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-hub/wingsplugings/todolist-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("docker-hub/wingsplugings/todolist-changed");
    assertThat(updatedArtifactStream).isInstanceOf(NexusArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(NEXUS.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("docker-hub"));
    assertThat(
        updatedArtifactStream.getArtifactStreamAttributes().getGroupId().equals("wingsplugings/todolist-changed_"));
    assertThat(
        updatedArtifactStream.getArtifactStreamAttributes().getImageName().equals("wingsplugings/todolist-changed_"));

    NexusArtifactStream updatedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(updatedNexusArtifactStream.getJobname()).isEqualTo("docker-hub");
    assertThat(updatedNexusArtifactStream.getImageName()).isEqualTo("wingsplugings/todolist-changed");

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  @Test
  public void shouldAddArtifactoryArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("generic-repo"));
    assertThat(
        savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern().equals("io/harness/todolist/todolist*"));

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("generic-repo");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern().equals("io/harness/todolist/todolist*"));
    assertThat(savedArtifactoryArtifactStream.getRepositoryType().equals("any"));

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldUpdateArtifactoryArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("generic-repo"));
    assertThat(
        savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern().equals("io/harness/todolist/todolist*"));

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("generic-repo");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern().equals("io/harness/todolist/todolist*"));
    assertThat(savedArtifactoryArtifactStream.getRepositoryType().equals("any"));

    savedArtifactoryArtifactStream.setName("Aritfactory_Changed");
    savedArtifactoryArtifactStream.setJobname("harness-rpm");
    savedArtifactoryArtifactStream.setArtifactPattern("todolist*");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedArtifactoryArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isEqualTo("Aritfactory_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("harness-rpm/todolist*");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harness-rpm/todolist*");
    assertThat(updatedArtifactStream).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("hanress-rpm"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern().equals("todolist*"));

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-rpm");
    assertThat(updatedArtifactoryArtifactStream.getArtifactPattern().equals("todolist*"));
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType().equals("any"));

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldAddArtifactoryMavenArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream =
        ArtifactoryArtifactStream.builder()
            .appId(APP_ID)
            .repositoryType("maven")
            .settingId(SETTING_ID)
            .jobname("harness-maven")
            .artifactPattern("io/harness/todolist/todolist/*/todolist*")
            .autoPopulate(true)
            .serviceId(SERVICE_ID)
            .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType().equals("maven"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("harness-maven"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern().equals(
        "io/harness/todolist/todolist/*/todolist*"));

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern().equals("io/harness/todolist/todolist/*/todolist*"));
    assertThat(savedArtifactoryArtifactStream.getRepositoryType().equals("maven"));
  }

  @Test
  public void shouldUpdateArtifactoryMavenArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream =
        ArtifactoryArtifactStream.builder()
            .appId(APP_ID)
            .repositoryType("maven")
            .settingId(SETTING_ID)
            .jobname("harness-maven")
            .artifactPattern("io/harness/todolist/todolist/*/todolist*")
            .autoPopulate(true)
            .serviceId(SERVICE_ID)
            .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType().equals("maven"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("harness-maven"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern().equals(
        "io/harness/todolist/todolist/*/todolist*"));

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern().equals("io/harness/todolist/todolist/*/todolist*"));
    assertThat(savedArtifactoryArtifactStream.getRepositoryType().equals("maven"));

    savedArtifactoryArtifactStream.setName("Aritfactory_Changed");
    savedArtifactoryArtifactStream.setJobname("harness-maven2");
    savedArtifactoryArtifactStream.setArtifactPattern("io/harness/todolist/todolist/*/todolist2*");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedArtifactoryArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isEqualTo("Aritfactory_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("io/harness/todolist/todolist/*/todolist2*");
    assertThat(updatedArtifactStream.getSourceName())
        .isEqualTo("harness-maven2/io/harness/todolist/todolist/*/todolist2*");
    assertThat(updatedArtifactStream).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("hanress-maven2"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern().equals(
        "io/harness/todolist/todolist/*/todolist2*"));

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven2");
    assertThat(
        updatedArtifactoryArtifactStream.getArtifactPattern().equals("io/harness/todolist/todolist/*/todolist2*"));
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType().equals("maven"));

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldAddArtifactoryDockerArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .jobname("docker")
                                                              .groupId("wingsplugins/todolist")
                                                              .imageName("wingsplugins/todolist")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("docker"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugins/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId().equals("wingsplugins/todolist"));

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("docker");
    assertThat(savedArtifactoryArtifactStream.getImageName().equals("wingsplugins/todolist"));
    assertThat(savedArtifactoryArtifactStream.getRepositoryType().equals("any"));

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldUpdateArtifactoryDockerArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .jobname("docker")
                                                              .groupId("wingsplugins/todolist")
                                                              .imageName("wingsplugins/todolist")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("docker"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugins/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId().equals("wingsplugins/todolist"));

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("docker");
    assertThat(savedArtifactoryArtifactStream.getImageName().equals("wingsplugins/todolist"));
    assertThat(savedArtifactoryArtifactStream.getRepositoryType().equals("any"));

    savedArtifactoryArtifactStream.setName("Aritfactory_Changed");
    savedArtifactoryArtifactStream.setJobname("docker-local");
    savedArtifactoryArtifactStream.setArtifactPattern("todolist*");
    savedArtifactoryArtifactStream.setGroupId("wingsplugins/todolist-changed");
    savedArtifactoryArtifactStream.setImageName("wingsplugins/todolist-changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedArtifactoryArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isEqualTo("Aritfactory_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-local/wingsplugins/todolist-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("docker-local/wingsplugins/todolist-changed");
    assertThat(updatedArtifactStream).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(ARTIFACTORY.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryType().equals("any"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("docker-local"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugins/todolist-changed"));

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("docker-local");
    assertThat(updatedArtifactoryArtifactStream.getImageName().equals("wingsplugins/todolist-changed"));
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType().equals("any"));

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldAddAmiArtifactStream() {
    AmiArtifactStream.Tag tag = new AmiArtifactStream.Tag();
    tag.setKey("name");
    tag.setValue("jenkins");
    AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .region("us-east-1")
                                              .tags(asList(tag))
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(amiArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam).isInstanceOf(AmiArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(AMI.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegion().equals("us-east-1"));
    AmiArtifactStream savedAmiArtifactStream = (AmiArtifactStream) savedArtifactSteam;
    assertThat(savedAmiArtifactStream.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  public void shouldUpdateAmiArtifactStream() {
    AmiArtifactStream.Tag tag = new AmiArtifactStream.Tag();
    tag.setKey("name");
    tag.setValue("jenkins");

    AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .region("us-east-1")
                                              .tags(asList(tag))
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(amiArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam).isInstanceOf(AmiArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(AMI.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegion().equals("us-east-1"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags().containsKey("name"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags().containsValue("jenkins"));

    AmiArtifactStream savedAmiArtifactStream = (AmiArtifactStream) savedArtifactSteam;
    assertThat(savedAmiArtifactStream.getRegion()).isEqualTo("us-east-1");

    AmiArtifactStream.Tag updatedTag = new AmiArtifactStream.Tag();
    updatedTag.setKey("name2");
    updatedTag.setValue("jenkins2");
    savedAmiArtifactStream.getTags().add(updatedTag);
    savedAmiArtifactStream.setRegion("us-west");

    ArtifactStream updatedAmiArtifactStream = artifactStreamService.update(savedAmiArtifactStream);

    assertThat(updatedAmiArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedAmiArtifactStream.getName()).isNotEmpty();
    assertThat(updatedAmiArtifactStream.getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(updatedAmiArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedAmiArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("us-west:name:jenkins");
    assertThat(updatedAmiArtifactStream.getSourceName()).isEqualTo("us-west:name:jenkins_name2:jenkins2");
    assertThat(updatedAmiArtifactStream).isInstanceOf(AmiArtifactStream.class);
    assertThat(updatedAmiArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(AMI.name()));
    assertThat(updatedAmiArtifactStream.getArtifactStreamAttributes().getRegion().equals("us-west"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags().containsKey("name"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags().containsValue("jenkins"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags().containsKey("name2"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags().containsValue("jenkins2"));

    AmiArtifactStream updatedArtifactStream = (AmiArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactStream.getRegion()).isEqualTo("us-west");

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  @Test
  public void shouldAddS3ArtifactStream() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(amazonS3ArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("harnessapps");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessapps/dev/todolist.war");
    assertThat(savedArtifactSteam).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(AMAZON_S3.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("harnessapps"));
    AmazonS3ArtifactStream savedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(savedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps");
    assertThat(savedAmazonS3ArtifactStream.getArtifactPaths().contains("dev/todolist.war"));
  }

  @Test
  public void shouldUpdateS3ArtifactStream() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(amazonS3ArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("harnessapps");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessapps/dev/todolist.war");
    assertThat(savedArtifactSteam).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(AMAZON_S3.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName().equals("harnessapps"));
    AmazonS3ArtifactStream savedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(savedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps");
    assertThat(savedAmazonS3ArtifactStream.getArtifactPaths().contains("dev/todolist.war"));

    savedAmazonS3ArtifactStream.setJobname("hanessapps-changed");
    savedAmazonS3ArtifactStream.setName("s3 stream");
    savedAmazonS3ArtifactStream.setArtifactPaths(asList("qa/todolist.war"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedAmazonS3ArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("s3 stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("hanessapps-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("hanessapps-changed/qa/todolist.war");
    assertThat(updatedArtifactStream).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(AMAZON_S3.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName().equals("harnessapps-changed"));
    AmazonS3ArtifactStream updatedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(updatedAmazonS3ArtifactStream.getJobname()).isEqualTo("hanessapps-changed");
    assertThat(updatedAmazonS3ArtifactStream.getArtifactPaths().contains("qa/todolist.war"));
  }

  @Test
  public void shouldAddDockerArtifactStream() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(DockerArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(DOCKER.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugins/todolist"));
    DockerArtifactStream savedDockerArtifactStream = (DockerArtifactStream) savedArtifactSteam;
    assertThat(savedDockerArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldUpdateDockerArtifactStream() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(DockerArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(DOCKER.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("wingsplugins/todolist"));
    DockerArtifactStream savedDockerArtifactStream = (DockerArtifactStream) savedArtifactSteam;
    assertThat(savedDockerArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");

    savedDockerArtifactStream.setImageName("harness/todolist");
    savedArtifactSteam.setName("Docker Stream");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedDockerArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Docker Stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("harness/todolist");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harness/todolist");
    assertThat(updatedArtifactStream).isInstanceOf(DockerArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(DOCKER.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getImageName().equals("harness/todolist"));
    DockerArtifactStream updatedDockerArtifactStream = (DockerArtifactStream) savedArtifactSteam;
    assertThat(updatedDockerArtifactStream.getImageName()).isEqualTo("harness/todolist");

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldAddEcrArtifactStream() {
    EcrArtifactStream dockerArtifactStream = EcrArtifactStream.builder()
                                                 .appId(APP_ID)
                                                 .settingId(SETTING_ID)
                                                 .imageName("todolist")
                                                 .region("us-east-1")
                                                 .autoPopulate(true)
                                                 .serviceId(SERVICE_ID)
                                                 .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolist");
    assertThat(savedArtifactSteam).isInstanceOf(EcrArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ECR.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    EcrArtifactStream savedEcrArtifactStream = (EcrArtifactStream) savedArtifactSteam;
    assertThat(savedEcrArtifactStream.getImageName()).isEqualTo("todolist");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldUpdateEcrArtifactStream() {
    EcrArtifactStream dockerArtifactStream = EcrArtifactStream.builder()
                                                 .appId(APP_ID)
                                                 .settingId(SETTING_ID)
                                                 .imageName("todolist")
                                                 .region("us-east-1")
                                                 .autoPopulate(true)
                                                 .serviceId(SERVICE_ID)
                                                 .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolist");
    assertThat(savedArtifactSteam).isInstanceOf(EcrArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ECR.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    EcrArtifactStream savedEcrArtifactStream = (EcrArtifactStream) savedArtifactSteam;

    savedEcrArtifactStream.setRegion("us-west");
    savedEcrArtifactStream.setName("Ecr Stream");
    savedEcrArtifactStream.setImageName("todolist-changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedEcrArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Ecr Stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("todolist-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("todolist-changed");

    assertThat(updatedArtifactStream).isInstanceOf(EcrArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(ECR.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getImageName().equals("todolist-changed"));
    EcrArtifactStream updatedEcrArtifactStream = (EcrArtifactStream) savedArtifactSteam;
    assertThat(updatedEcrArtifactStream.getImageName()).isEqualTo("todolist-changed");

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldAddGcrArtifactStream() {
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(gcrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("exploration-161417/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("gcr.io/exploration-161417/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(GcrArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(GCR.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("exploration-161417/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryHostName().equals("gcr.io"));
    GcrArtifactStream savedGcrArtifactStream = (GcrArtifactStream) savedArtifactSteam;
    assertThat(savedGcrArtifactStream.getDockerImageName()).isEqualTo("exploration-161417/todolist");
    assertThat(savedGcrArtifactStream.getRegistryHostName()).isEqualTo("gcr.io");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldUpdateGcrArtifactStream() {
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(gcrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("exploration-161417/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("gcr.io/exploration-161417/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(GcrArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(GCR.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName().equals("exploration-161417/todolist"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryHostName().equals("gcr.io"));
    GcrArtifactStream savedGcrArtifactStream = (GcrArtifactStream) savedArtifactSteam;
    assertThat(savedGcrArtifactStream.getDockerImageName()).isEqualTo("exploration-161417/todolist");
    assertThat(savedGcrArtifactStream.getRegistryHostName()).isEqualTo("gcr.io");

    savedGcrArtifactStream.setDockerImageName("exploration-161417/todolist-changed");
    savedGcrArtifactStream.setRegistryHostName("gcr.io");
    savedGcrArtifactStream.setName("Gcr Stream");

    ArtifactStream updatedArtifactSteam = artifactStreamService.update(savedGcrArtifactStream);
    assertThat(updatedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(updatedArtifactSteam.getName()).isNotEmpty().isEqualTo("Gcr Stream");
    assertThat(updatedArtifactSteam.getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(updatedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactSteam.getArtifactDisplayName(""))
        .isNotEmpty()
        .contains("exploration-161417/todolist-changed");
    assertThat(updatedArtifactSteam.getSourceName()).isEqualTo("gcr.io/exploration-161417/todolist-changed");
    assertThat(updatedArtifactSteam).isInstanceOf(GcrArtifactStream.class);
    assertThat(updatedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(GCR.name()));
    assertThat(updatedArtifactSteam.getArtifactStreamAttributes().getImageName().equals(
        "exploration-161417/todolist-changed"));
    assertThat(updatedArtifactSteam.getArtifactStreamAttributes().getRegistryHostName().equals("gcr.io"));

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldAddAcrArtifactStream() {
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(acrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("harnessqa/nginx");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessqa/nginx");
    assertThat(savedArtifactSteam).isInstanceOf(AcrArtifactStream.class);
    assertThat(
        savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ArtifactStreamType.ACR.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getSubscriptionId().equals(
        "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryName().equals("nginx"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryName().equals("harnessqa"));
    AcrArtifactStream savedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(savedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedAcrArtifactStream.getRepositoryName()).isEqualTo("nginx");
    assertThat(savedAcrArtifactStream.getRegistryName().equals("harnessqa"));

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldUpdateAcrArtifactStream() {
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(acrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("harnessqa/nginx");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessqa/nginx");
    assertThat(savedArtifactSteam).isInstanceOf(AcrArtifactStream.class);
    assertThat(
        savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType().equals(ArtifactStreamType.ACR.name()));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getSubscriptionId().equals(
        "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryName().equals("nginx"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryName().equals("harnessqa"));
    AcrArtifactStream savedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(savedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedAcrArtifactStream.getRepositoryName()).isEqualTo("nginx");
    assertThat(savedAcrArtifactStream.getRegistryName().equals("harnessqa"));

    savedAcrArtifactStream.setRegistryName("harnessprod");
    savedAcrArtifactStream.setRepositoryName("istio");
    savedAcrArtifactStream.setName("Acr Stream");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedAcrArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty();
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("harnessprod/istio");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harnessprod/istio");
    assertThat(updatedArtifactStream).isInstanceOf(AcrArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType().equals(
        ArtifactStreamType.ACR.name()));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getSubscriptionId().equals(
        "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryName().equals("istio"));
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRegistryName().equals("harnessprod"));

    AcrArtifactStream updatedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(updatedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(updatedAcrArtifactStream.getRepositoryName()).isEqualTo("istio");
    assertThat(updatedAcrArtifactStream.getRegistryName().equals("harnessprod"));

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  public void shouldListArtifactStreams() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedJenkinsArtifactSteam = artifactStreamService.create(jenkinsArtifactStream);

    assertThat(savedJenkinsArtifactSteam).isNotNull();

    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    PageRequest<ArtifactStream> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, APP_ID).build();

    List<ArtifactStream> artifactStreams = artifactStreamService.list(pageRequest);
    assertThat(artifactStreams).isNotEmpty().size().isEqualTo(2);
    assertThat(artifactStreams)
        .extracting(artifactStream -> artifactStream.getArtifactStreamType())
        .contains(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.ARTIFACTORY.name());
  }

  @Test
  public void shouldGetArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    ArtifactStream artifactStream = artifactStreamService.get(APP_ID, savedArtifactStream.getUuid());
    assertThat(artifactStream.getUuid()).isEqualTo(savedArtifactStream.getUuid());
    assertThat(artifactStream.getName()).isNotEmpty();
  }

  @Test
  public void shouldDeleteArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();
    assertThat(artifactStreamService.delete(APP_ID, savedArtifactStream.getUuid())).isTrue();
  }

  @Test(expected = WingsException.class)
  public void shouldNotDeleteArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    when(triggerService.getTriggersHasArtifactStreamAction(APP_ID, savedArtifactStream.getUuid()))
        .thenReturn(
            Collections.singletonList(software.wings.beans.trigger.Trigger.builder().name(TRIGGER_NAME).build()));
    assertThat(artifactStreamService.delete(APP_ID, savedArtifactStream.getUuid())).isTrue();
  }

  @Test
  public void shouldGetDockerArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(DockerConfig.builder()
                        .dockerRegistryUrl("http://hub.docker.com/")
                        .username("username")
                        .password("password".toCharArray())
                        .accountId(ACCOUNT_ID)
                        .build());
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(DOCKER.name());
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://hub.docker.com/", "wingsplugins/todolist");
  }

  @Test
  public void shouldGetDockerArtifactSourcePropertiesWhenArtifactStreamDeleted() {
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID);
    assertThat(artifactSourceProperties).isEmpty();
  }

  @Test
  public void shouldGetGcrArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(GcpConfig.builder().accountId(ACCOUNT_ID).build());
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(gcrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("gcr.io", "exploration-161417/todolist");
  }

  @Test
  public void shouldGetAcrArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(AzureConfig.builder().accountId(ACCOUNT_ID).build());

    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(acrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("harnessqa", "nginx");
  }

  @Test
  public void shouldGetEcrArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(AzureConfig.builder().accountId(ACCOUNT_ID).build());

    EcrArtifactStream ecrArtifactStream = EcrArtifactStream.builder()
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .imageName("todolist")
                                              .region("us-east-1")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(ecrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties).containsOnlyKeys(ARTIFACT_SOURCE_REPOSITORY_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("todolist");
  }

  @Test
  public void shouldGetJenkinsArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(JenkinsConfig.builder()
                        .jenkinsUrl("http://jenkins.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(jenkinsArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://jenkins.software");
  }

  @Test
  public void shouldGetBabmooArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(JenkinsConfig.builder()
                        .jenkinsUrl("http://bamboo.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(bambooArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://bamboo.software");
  }

  @Test
  public void shouldGetNexusArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://bamboo.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .groupId("io.harness.test")
                                                  .artifactPaths(asList("todolist"))
                                                  .autoPopulate(true)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://bamboo.software");
  }

  @Test
  public void shouldGetNexusDockerArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://bamboo.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .imageName("wingsplugins/todolist")
                                                  .autoPopulate(true)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://bamboo.software", "wingsplugins/todolist");
  }

  @Test
  public void shouldGetArtifactoryArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://artifactory.com")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://artifactory.com");
  }

  @Test
  public void shouldGetArtifactoryDockerArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://artifactory.com")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    ArtifactoryArtifactStream artifactoryArtifactStream = buildArtifactoryStream();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, APP_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://artifactory.com", "wingsplugins/todolist");
  }

  private ArtifactoryArtifactStream buildArtifactoryStream() {
    return ArtifactoryArtifactStream.builder()
        .appId(APP_ID)
        .repositoryType("any")
        .jobname("docker")
        .settingId(SETTING_ID)
        .imageName("wingsplugins/todolist")
        .autoPopulate(true)
        .serviceId(SERVICE_ID)
        .build();
  }

  @Test
  public void shouldListArtifactStreamIdsofService() {
    ArtifactStream savedArtifactSteam = artifactStreamService.create(buildArtifactoryStream());
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(artifactStreamService.fetchArtifactStreamIdsForService(APP_ID, SERVICE_ID))
        .isNotEmpty()
        .contains(savedArtifactSteam.getUuid());
  }

  @Test
  public void shouldListArtifactStreamsofService() {
    ArtifactStream savedArtifactSteam = artifactStreamService.create(buildArtifactoryStream());
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(artifactStreamService.fetchArtifactStreamsForService(APP_ID, SERVICE_ID))
        .isNotEmpty()
        .extracting(ArtifactStream::getUuid)
        .isNotNull()
        .contains(savedArtifactSteam.getUuid());
  }
}
