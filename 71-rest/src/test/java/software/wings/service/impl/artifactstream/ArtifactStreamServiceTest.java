package software.wings.service.impl.artifactstream;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
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
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.common.Constants.ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_USER_NAME_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
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
import software.wings.beans.Service;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Action;
import software.wings.beans.artifact.CustomArtifactStream.Script;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.NexusConfig;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.scheduler.ServiceJobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.ArtifactType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArtifactStreamServiceTest extends WingsBaseTest {
  @Mock private BackgroundJobScheduler backgroundJobScheduler;
  @Mock private ServiceJobScheduler serviceJobScheduler;
  @Mock private YamlPushService yamlPushService;
  @Mock private AppService appService;
  @Mock private BuildSourceService buildSourceService;
  @Mock private TriggerService triggerService;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactService artifactService;
  @InjectMocks @Inject private ArtifactStreamService artifactStreamService;

  @Test
  public void shouldGetSupportedBuildSourceTypes() {
    // For DOCKER Service Artifact Type
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().appId(APP_ID).artifactType(ArtifactType.DOCKER).uuid(SERVICE_ID).build())
        .thenReturn(Service.builder().appId(APP_ID).artifactType(ArtifactType.OTHER).uuid(SERVICE_ID).build());

    Map<String, String> supportedBuildSourceTypes =
        artifactStreamService.getSupportedBuildSourceTypes(APP_ID, SERVICE_ID);
    assertThat(supportedBuildSourceTypes.containsKey(CUSTOM));
    assertThat(supportedBuildSourceTypes.containsValue(CUSTOM));

    supportedBuildSourceTypes = artifactStreamService.getSupportedBuildSourceTypes(APP_ID, SERVICE_ID);
    assertThat(supportedBuildSourceTypes.containsKey(CUSTOM));
    assertThat(supportedBuildSourceTypes.containsValue(CUSTOM));
  }

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("todolistwar");

    JenkinsArtifactStream savedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(savedJenkinsArtifactStream.getJobname()).isEqualTo("todolistwar");
    assertThat(savedJenkinsArtifactStream.getArtifactPaths()).contains("target/todolist.war");

    verify(serviceJobScheduler).ensureJob__UnderConstruction(any(JobDetail.class), any(Trigger.class));
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("todolistwar");

    JenkinsArtifactStream savedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(savedJenkinsArtifactStream.getJobname()).isEqualTo("todolistwar");
    assertThat(savedJenkinsArtifactStream.getArtifactPaths()).contains("target/todolist.war");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("todoliswar_changed");
    JenkinsArtifactStream updatedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(updatedJenkinsArtifactStream.getJobname()).isEqualTo("todoliswar_changed");
    assertThat(updatedJenkinsArtifactStream.getArtifactPaths()).contains("*WAR_Changed");

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(artifactService).deleteWhenArtifactSourceNameChanged(jenkinsArtifactStream);
    verify(triggerService).updateByApp(APP_ID);
  }

  @Test
  public void shouldAddBambooArtifactStream() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createBambooArtifactStream(bambooArtifactStream);
    BambooArtifactStream savedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist.war");

    verify(serviceJobScheduler).ensureJob__UnderConstruction(any(JobDetail.class), any(Trigger.class));
    verify(appService).getAccountIdByAppId(APP_ID);
  }

  @Test
  public void shouldUpdateBambooArtifactStream() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createBambooArtifactStream(bambooArtifactStream);

    BambooArtifactStream savedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist.war");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("TOD-TOD_Changed");
    BambooArtifactStream updatedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(updatedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD_Changed");
    assertThat(updatedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist_changed.war");

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(artifactService).deleteWhenArtifactSourceNameChanged(bambooArtifactStream);
    verify(triggerService).updateByApp(APP_ID);
  }

  private ArtifactStream createBambooArtifactStream(BambooArtifactStream bambooArtifactStream) {
    ArtifactStream savedArtifactSteam = artifactStreamService.create(bambooArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.getArtifactDisplayName("")).isNotEmpty().contains("TOD-TOD");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("TOD-TOD");
    assertThat(savedArtifactSteam).isInstanceOf(BambooArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("TOD-TOD");
    return savedArtifactSteam;
  }

  @Test
  public void shouldAddNexusArtifactStream() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStream();

    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("io.harness.test");
    assertThat(savedNexusArtifactStream.getArtifactPaths()).contains("todolist");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("releases");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId()).isEqualTo("io.harness.test");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactName()).isEqualTo("todolist");
    return savedArtifactSteam;
  }

  @Test
  public void shouldUpdateNexusArtifactStream() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStream();
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getArtifactPaths()).contains("todolist");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("snapshots");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getGroupId()).isEqualTo("io.harness.test.changed");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactName()).isEqualTo("todolist-changed");
    NexusArtifactStream updatedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(updatedNexusArtifactStream.getJobname()).isEqualTo("snapshots");
    assertThat(updatedNexusArtifactStream.getArtifactPaths()).contains("todolist-changed");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("docker-private");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactName()).isEmpty();
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("docker-private");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactName()).isEmpty();
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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("docker-hub");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getGroupId())
        .isEqualTo("wingsplugings/todolist-changed");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getImageName())
        .isEqualTo("wingsplugings/todolist-changed");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("generic-repo");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("generic-repo");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern()).isEqualTo("io/harness/todolist/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("generic-repo");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("generic-repo");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern()).isEqualTo("io/harness/todolist/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("harness-rpm");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern()).isEqualTo("todolist*");

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-rpm");
    assertThat(updatedArtifactoryArtifactStream.getArtifactPattern()).isEqualTo("todolist*");
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("maven");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("harness-maven");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("maven");
  }

  @Test
  public void shouldUpdateArtifactoryMavenArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream =
        ArtifactoryArtifactStream.builder()
            .appId(APP_ID)
            .repositoryType("any")
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("harness-maven");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("harness-maven2");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist2*");

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven2");
    assertThat(updatedArtifactoryArtifactStream.getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist2*");
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("docker");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugins/todolist");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("docker");
    assertThat(savedArtifactoryArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("docker");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugins/todolist");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("docker");
    assertThat(savedArtifactoryArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("docker-local");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName())
        .isEqualTo("wingsplugins/todolist-changed");

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("docker-local");
    assertThat(updatedArtifactoryArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist-changed");
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("any");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags()).containsKey("name");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags()).containsValue(asList("jenkins"));

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
    assertThat(updatedAmiArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(updatedAmiArtifactStream.getArtifactStreamAttributes().getRegion()).isEqualTo("us-west");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags()).containsKey("name");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags()).containsValue(asList("jenkins"));
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags()).containsKey("name2");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getTags()).containsValue(asList("jenkins2"));

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("harnessapps");
    AmazonS3ArtifactStream savedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(savedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps");
    assertThat(savedAmazonS3ArtifactStream.getArtifactPaths()).contains("dev/todolist.war");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getJobName()).isEqualTo("harnessapps");
    AmazonS3ArtifactStream savedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(savedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps");
    assertThat(savedAmazonS3ArtifactStream.getArtifactPaths()).contains("dev/todolist.war");

    savedAmazonS3ArtifactStream.setJobname("harnessapps-changed");
    savedAmazonS3ArtifactStream.setName("s3 stream");
    savedAmazonS3ArtifactStream.setArtifactPaths(asList("qa/todolist.war"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedAmazonS3ArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("s3 stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedArtifactStream.getArtifactDisplayName("")).isNotEmpty().contains("harnessapps-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harnessapps-changed/qa/todolist.war");
    assertThat(updatedArtifactStream).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getJobName()).isEqualTo("harnessapps-changed");
    AmazonS3ArtifactStream updatedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(updatedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps-changed");
    assertThat(updatedAmazonS3ArtifactStream.getArtifactPaths()).contains("qa/todolist.war");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getImageName()).isEqualTo("harness/todolist");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("todolist");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName()).isEqualTo("todolist");
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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getImageName()).isEqualTo("todolist-changed");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName())
        .isEqualTo("exploration-161417/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryHostName()).isEqualTo("gcr.io");
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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getImageName())
        .isEqualTo("exploration-161417/todolist");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryHostName()).isEqualTo("gcr.io");
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
    assertThat(updatedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(updatedArtifactSteam.getArtifactStreamAttributes().getImageName())
        .isEqualTo("exploration-161417/todolist-changed");
    assertThat(updatedArtifactSteam.getArtifactStreamAttributes().getRegistryHostName()).isEqualTo("gcr.io");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getSubscriptionId())
        .isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryName()).isEqualTo("nginx");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryName()).isEqualTo("harnessqa");
    AcrArtifactStream savedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(savedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedAcrArtifactStream.getRepositoryName()).isEqualTo("nginx");
    assertThat(savedAcrArtifactStream.getRegistryName()).isEqualTo("harnessqa");

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
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getSubscriptionId())
        .isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRepositoryName()).isEqualTo("nginx");
    assertThat(savedArtifactSteam.getArtifactStreamAttributes().getRegistryName()).isEqualTo("harnessqa");
    AcrArtifactStream savedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(savedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedAcrArtifactStream.getRepositoryName()).isEqualTo("nginx");
    assertThat(savedAcrArtifactStream.getRegistryName()).isEqualTo("harnessqa");

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
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getSubscriptionId())
        .isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRepositoryName()).isEqualTo("istio");
    assertThat(updatedArtifactStream.getArtifactStreamAttributes().getRegistryName()).isEqualTo("harnessprod");

    AcrArtifactStream updatedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(updatedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(updatedAcrArtifactStream.getRepositoryName()).isEqualTo("istio");
    assertThat(updatedAcrArtifactStream.getRegistryName()).isEqualTo("harnessprod");

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
        .containsKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY,
            ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY,
            ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY,
            ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY,
            ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY,
            ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
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

  @Test
  public void shouldCRUDCustomArtifactStream() {
    ArtifactStream customArtifactStream =
        CustomArtifactStream.builder()
            .appId(APP_ID)
            .serviceId(SERVICE_ID)
            .name("Custom Artifact Stream" + System.currentTimeMillis())
            .scripts(Arrays.asList(CustomArtifactStream.Script.builder()
                                       .action(Action.FETCH_VERSIONS)
                                       .scriptString("echo Hello World!! and echo ${secrets.getValue(My Secret)}")
                                       .build()))
            .build();

    ArtifactStream savedArtifactSteam = artifactStreamService.create(customArtifactStream);

    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo(savedArtifactSteam.getName());
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.CUSTOM.name());
    CustomArtifactStream savedCustomArtifactStream = (CustomArtifactStream) savedArtifactSteam;
    assertThat(savedCustomArtifactStream.getScripts()).isNotEmpty();
    Script script = savedCustomArtifactStream.getScripts().stream().findFirst().orElse(null);
    assertThat(script.getAction()).isEqualTo(Action.FETCH_VERSIONS);
    assertThat(script.getScriptString()).isEqualTo("echo Hello World!! and echo ${secrets.getValue(My Secret)}");

    assertThat(artifactStreamService.fetchArtifactStreamsForService(APP_ID, SERVICE_ID))
        .isNotEmpty()
        .extracting(ArtifactStream::getUuid)
        .isNotNull()
        .contains(savedArtifactSteam.getUuid());

    script.setScriptString("Welcome to harness");
    savedCustomArtifactStream.setScripts(Arrays.asList(script));
    savedCustomArtifactStream.setName("Name Changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedArtifactSteam);

    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo(updatedArtifactStream.getName());
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ArtifactStreamType.CUSTOM.name());
    CustomArtifactStream updatedCustomArtifactStream = (CustomArtifactStream) savedArtifactSteam;
    Script updatedScript = updatedCustomArtifactStream.getScripts().stream().findFirst().orElse(null);
    assertThat(updatedScript.getScriptString()).isEqualTo("Welcome to harness");

    artifactStreamService.delete(APP_ID, updatedArtifactStream.getUuid());

    assertThat(artifactStreamService.get(APP_ID, updatedArtifactStream.getUuid())).isNull();

    verify(artifactService, times(0)).deleteWhenArtifactSourceNameChanged(customArtifactStream);
    verify(triggerService).updateByApp(APP_ID);
  }
}
