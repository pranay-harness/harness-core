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
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_USER_NAME_KEY;
import static software.wings.beans.template.artifactsource.CustomRepositoryMapping.AttributeMapping.builder;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
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
import software.wings.beans.template.artifactsource.CustomRepositoryMapping;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArtifactStreamServiceTest extends WingsBaseTest {
  private static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";
  private static final String ANOTHER_SETTING_ID = "ANOTHER_SETTING_ID";
  @Mock private BackgroundJobScheduler backgroundJobScheduler;
  @Mock private YamlPushService yamlPushService;
  @Mock private AppService appService;
  @Mock private BuildSourceService buildSourceService;
  @Mock private TriggerService triggerService;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @InjectMocks @Inject private ArtifactStreamService artifactStreamService;

  private ArtifactStream createArtifactStream(ArtifactStream artifactStream) {
    ArtifactStream savedArtifactSteam = artifactStreamService.create(artifactStream);
    when(artifactStreamServiceBindingService.listArtifactStreams(artifactStream.getAppId(), SERVICE_ID))
        .thenReturn(asList(artifactStream));
    when(artifactStreamServiceBindingService.listArtifactStreamIds(artifactStream.getAppId(), SERVICE_ID))
        .thenReturn(asList(artifactStream.getUuid()));
    return savedArtifactSteam;
  }

  @Test
  @Category(UnitTests.class)
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
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute().withAccountId(ACCOUNT_ID).build();
    when(settingsService.get(SETTING_ID)).thenReturn(settingAttribute);
    when(settingsService.fetchAccountIdBySettingId(SETTING_ID)).thenReturn(ACCOUNT_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddJenkinsArtifactStream() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedArtifactSteam = createArtifactStream(jenkinsArtifactStream);
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    validateJenkinsArtifactStreamOnCreate(savedArtifactSteam);
    verify(appService).getAccountIdByAppId(APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddJenkinsArtifactStreamAtConnectorLevel() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStreamAtConnectorLevel();
    ArtifactStream savedArtifactSteam = createArtifactStream(jenkinsArtifactStream);
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(GLOBAL_APP_ID);
    validateJenkinsArtifactStreamOnCreate(savedArtifactSteam);
  }

  private void validateJenkinsArtifactStreamOnCreate(ArtifactStream savedArtifactSteam) {
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    String artifactDisplayName = savedArtifactSteam.fetchArtifactDisplayName("40");
    assertThat(artifactDisplayName).isNotEmpty().contains("todolistwar");
    String[] values = artifactDisplayName.split("_");
    assertThat(values).hasSize(3);
    assertThat(values[0]).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam).isInstanceOf(JenkinsArtifactStream.class);
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam.getKeywords().size()).isEqualTo(2);
    assertThat(savedArtifactSteam.getKeywords()).contains("todolistwar", "jenkins");
    JenkinsArtifactStream savedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(savedJenkinsArtifactStream.getJobname()).isEqualTo("todolistwar");
    assertThat(savedJenkinsArtifactStream.getArtifactPaths()).contains("target/todolist.war");
  }

  private JenkinsArtifactStream getJenkinsStream() {
    return JenkinsArtifactStream.builder()
        .sourceName("todolistwar")
        .settingId(SETTING_ID)
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .jobname("todolistwar")
        .autoPopulate(true)
        .serviceId(SERVICE_ID)
        .artifactPaths(asList("target/todolist.war"))
        .build();
  }

  private JenkinsArtifactStream getJenkinsStreamAtConnectorLevel() {
    return JenkinsArtifactStream.builder()
        .sourceName("todolistwar")
        .settingId(SETTING_ID)
        .accountId(ACCOUNT_ID)
        .appId(GLOBAL_APP_ID)
        .jobname("todolistwar")
        .autoPopulate(true)
        .artifactPaths(asList("target/todolist.war"))
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateJenkinsArtifactStream() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedArtifactSteam = createArtifactStream(jenkinsArtifactStream);
    JenkinsArtifactStream savedJenkinsArtifactStream = validateJenkinsArtifactStream(savedArtifactSteam, APP_ID);
    updateAndValidateJenkinsArtifactStream(
        (JenkinsArtifactStream) savedArtifactSteam, savedJenkinsArtifactStream, APP_ID);

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(artifactService).deleteWhenArtifactSourceNameChanged(jenkinsArtifactStream);
    verify(triggerService).updateByArtifactStream(savedJenkinsArtifactStream.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateJenkinsArtifactStreamAtConnectorLevel() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStreamAtConnectorLevel();
    ArtifactStream savedArtifactSteam = createArtifactStream(jenkinsArtifactStream);
    JenkinsArtifactStream savedJenkinsArtifactStream = validateJenkinsArtifactStream(savedArtifactSteam, GLOBAL_APP_ID);
    updateAndValidateJenkinsArtifactStream(
        (JenkinsArtifactStream) savedArtifactSteam, savedJenkinsArtifactStream, GLOBAL_APP_ID);

    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(artifactService).deleteWhenArtifactSourceNameChanged(jenkinsArtifactStream);
    //    verify(triggerService).updateByApp(APP_ID);
  }

  @NotNull
  private JenkinsArtifactStream validateJenkinsArtifactStream(ArtifactStream savedArtifactSteam, String appId) {
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);

    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("todolistwar");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolistwar");
    assertThat(savedArtifactSteam).isInstanceOf(JenkinsArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("todolistwar");

    JenkinsArtifactStream savedJenkinsArtifactStream = (JenkinsArtifactStream) savedArtifactSteam;
    assertThat(savedJenkinsArtifactStream.getJobname()).isEqualTo("todolistwar");
    assertThat(savedJenkinsArtifactStream.getArtifactPaths()).contains("target/todolist.war");
    return savedJenkinsArtifactStream;
  }

  private void updateAndValidateJenkinsArtifactStream(
      JenkinsArtifactStream savedArtifactSteam, JenkinsArtifactStream savedJenkinsArtifactStream, String appId) {
    savedJenkinsArtifactStream.setName("JekinsName_Changed");
    savedJenkinsArtifactStream.setJobname("todoliswar_changed");
    savedJenkinsArtifactStream.setArtifactPaths(asList("*WAR_Changed"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedJenkinsArtifactStream);

    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("JekinsName_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);

    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("todoliswar_changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("todoliswar_changed");
    assertThat(updatedArtifactStream).isInstanceOf(JenkinsArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(JENKINS.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("todoliswar_changed");
    assertThat(updatedArtifactStream.getKeywords().size()).isEqualTo(3);
    assertThat(updatedArtifactStream.getKeywords()).contains("jekinsname_changed", "todoliswar_changed", "jenkins");
    JenkinsArtifactStream updatedJenkinsArtifactStream = savedArtifactSteam;
    assertThat(updatedJenkinsArtifactStream.getJobname()).isEqualTo("todoliswar_changed");
    assertThat(updatedJenkinsArtifactStream.getArtifactPaths()).contains("*WAR_Changed");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddBambooArtifactStream() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createBambooArtifactStream(bambooArtifactStream, APP_ID);
    BambooArtifactStream savedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist.war");

    verify(appService).getAccountIdByAppId(APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddBambooArtifactStreamAtConnectorLevel() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(GLOBAL_APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createBambooArtifactStream(bambooArtifactStream, GLOBAL_APP_ID);
    BambooArtifactStream savedBambooArtifactStream = (BambooArtifactStream) savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist.war");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateBambooArtifactStream() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createBambooArtifactStream(bambooArtifactStream, APP_ID);
    updateAndValidateBambooArtifactStream((BambooArtifactStream) savedArtifactSteam, APP_ID);

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(artifactService).deleteWhenArtifactSourceNameChanged(bambooArtifactStream);
    verify(triggerService).updateByArtifactStream(savedArtifactSteam.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateBambooArtifactStreamAtConnectorLevel() {
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(GLOBAL_APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createBambooArtifactStream(bambooArtifactStream, GLOBAL_APP_ID);

    updateAndValidateBambooArtifactStream((BambooArtifactStream) savedArtifactSteam, GLOBAL_APP_ID);

    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
    verify(artifactService).deleteWhenArtifactSourceNameChanged(bambooArtifactStream);
    //    verify(triggerService).updateByApp(APP_ID);
  }

  private void updateAndValidateBambooArtifactStream(BambooArtifactStream savedArtifactSteam, String appId) {
    BambooArtifactStream savedBambooArtifactStream = savedArtifactSteam;
    assertThat(savedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD");
    assertThat(savedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist.war");

    savedBambooArtifactStream.setName("Bamboo_Changed");
    savedBambooArtifactStream.setJobname("TOD-TOD_Changed");
    savedBambooArtifactStream.setArtifactPaths(asList("artifacts/todolist_changed.war"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedBambooArtifactStream);

    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Bamboo_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);

    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("TOD-TOD_Changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("TOD-TOD_Changed");
    assertThat(updatedArtifactStream).isInstanceOf(BambooArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("TOD-TOD_Changed");
    BambooArtifactStream updatedBambooArtifactStream = savedArtifactSteam;
    assertThat(updatedBambooArtifactStream.getJobname()).isEqualTo("TOD-TOD_Changed");
    assertThat(updatedBambooArtifactStream.getArtifactPaths()).contains("artifacts/todolist_changed.war");
  }

  private ArtifactStream createBambooArtifactStream(BambooArtifactStream bambooArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(bambooArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("TOD-TOD");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("TOD-TOD");
    assertThat(savedArtifactSteam).isInstanceOf(BambooArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(BAMBOO.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("TOD-TOD");
    return savedArtifactSteam;
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddNexusArtifactStream() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStream();
    validateNexusArtifactStream(savedArtifactSteam, APP_ID);
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("io.harness.test");
    assertThat(savedNexusArtifactStream.getArtifactPaths()).contains("todolist");
    assertThat(savedNexusArtifactStream.getRepositoryFormat()).isEqualTo(RepositoryFormat.maven.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddNexusArtifactStreamAtConnectorLevel() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStreamAtConnectorLevel();
    validateNexusArtifactStream(savedArtifactSteam, GLOBAL_APP_ID);
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("io.harness.test");
    assertThat(savedNexusArtifactStream.getArtifactPaths()).contains("todolist");
    assertThat(savedNexusArtifactStream.getRepositoryFormat()).isEqualTo(RepositoryFormat.maven.name());
  }

  private ArtifactStream createNexusArtifactStream() {
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .groupId("io.harness.test")
                                                  .artifactPaths(asList("todolist"))
                                                  .autoPopulate(true)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    return savedArtifactSteam;
  }

  private ArtifactStream createNexusArtifactStreamAtConnectorLevel() {
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(GLOBAL_APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .groupId("io.harness.test")
                                                  .artifactPaths(asList("todolist"))
                                                  .autoPopulate(true)
                                                  .repositoryType("maven")
                                                  .build();
    ArtifactStream savedArtifactSteam = artifactStreamService.create(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    return savedArtifactSteam;
  }

  private void validateNexusArtifactStream(ArtifactStream savedArtifactSteam, String appId) {
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("releases/io.harness.test/todolist__");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("releases/io.harness.test/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(NexusArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("releases");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getGroupId()).isEqualTo("io.harness.test");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactName()).isEqualTo("todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateNexusArtifactStream() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStream();
    updateNexusArtifactStreamAndValidate((NexusArtifactStream) savedArtifactSteam, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateNexusArtifactStreamAtConnectorLevel() {
    ArtifactStream savedArtifactSteam = createNexusArtifactStreamAtConnectorLevel();
    updateNexusArtifactStreamAndValidate((NexusArtifactStream) savedArtifactSteam, GLOBAL_APP_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void shouldNotUpdateNexusArtifactStreamWithDifferentRepositoryFormat() {
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) createNexusArtifactStreamAtConnectorLevel();
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getArtifactPaths()).contains("todolist");
    assertThat(savedNexusArtifactStream.getRepositoryFormat()).isEqualTo(RepositoryFormat.maven.name());

    savedNexusArtifactStream.setRepositoryFormat(RepositoryFormat.docker.name());
    artifactStreamService.update(savedNexusArtifactStream);
  }

  private void updateNexusArtifactStreamAndValidate(NexusArtifactStream savedArtifactSteam, String appId) {
    NexusArtifactStream savedNexusArtifactStream = savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("releases");
    assertThat(savedNexusArtifactStream.getArtifactPaths()).contains("todolist");
    assertThat(savedNexusArtifactStream.getRepositoryFormat()).isEqualTo(RepositoryFormat.maven.name());

    savedNexusArtifactStream.setName("Nexus_Changed");
    savedNexusArtifactStream.setJobname("snapshots");
    savedNexusArtifactStream.setGroupId("io.harness.test.changed");
    savedNexusArtifactStream.setArtifactPaths(asList("todolist-changed"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedNexusArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty();
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("snapshots/io.harness.test.changed/todolist-changed__");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("snapshots/io.harness.test.changed/todolist-changed");
    assertThat(updatedArtifactStream).isInstanceOf(NexusArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("snapshots");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getGroupId()).isEqualTo("io.harness.test.changed");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactName()).isEqualTo("todolist-changed");
    NexusArtifactStream updatedNexusArtifactStream = savedArtifactSteam;
    assertThat(updatedNexusArtifactStream.getJobname()).isEqualTo("snapshots");
    assertThat(updatedNexusArtifactStream.getArtifactPaths()).contains("todolist-changed");

    if (appId.equals(APP_ID)) {
      verify(appService, times(2)).getAccountIdByAppId(appId);
      verify(yamlPushService, times(2))
          .pushYamlChangeSet(any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(),
              anyBoolean(), anyBoolean());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddNexusDockerArtifactStream() {
    NexusArtifactStream nexusDockerArtifactStream = NexusArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("docker-private")
                                                        .groupId("wingsplugings/todolist")
                                                        .imageName("wingsplugings/todolist")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .repositoryFormat(RepositoryFormat.docker.name())
                                                        .build();
    validateNexusDockerArtifactStream(nexusDockerArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddNexusDockerArtifactStreamAtConnectorLevel() {
    NexusArtifactStream nexusDockerArtifactStream = NexusArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(GLOBAL_APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("docker-private")
                                                        .groupId("wingsplugings/todolist")
                                                        .imageName("wingsplugings/todolist")
                                                        .autoPopulate(true)
                                                        .repositoryFormat(RepositoryFormat.docker.name())
                                                        .build();
    validateNexusDockerArtifactStream(nexusDockerArtifactStream, GLOBAL_APP_ID);
  }

  private void validateNexusDockerArtifactStream(NexusArtifactStream nexusDockerArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(nexusDockerArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-private/wingsplugings/todolist__");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker-private/wingsplugings/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(NexusArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("docker-private");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactName()).isEmpty();
    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("docker-private");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedNexusArtifactStream.getImageName()).isEqualTo("wingsplugings/todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateNexusDockerArtifactStream() {
    NexusArtifactStream nexusDockerArtifactStream = NexusArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("docker-private")
                                                        .groupId("wingsplugings/todolist")
                                                        .imageName("wingsplugings/todolist")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .dockerRegistryUrl("https://nexus3.harness.io")
                                                        .build();
    updateNexusDockerArtifactStreamAndValidate(nexusDockerArtifactStream, APP_ID);
    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateNexusDockerArtifactStreamAtConnectorLevel() {
    NexusArtifactStream nexusDockerArtifactStream = NexusArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(GLOBAL_APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("docker-private")
                                                        .groupId("wingsplugings/todolist")
                                                        .imageName("wingsplugings/todolist")
                                                        .autoPopulate(true)
                                                        .dockerRegistryUrl("https://nexus3.harness.io")
                                                        .repositoryType("docker")
                                                        .build();
    updateNexusDockerArtifactStreamAndValidate(nexusDockerArtifactStream, GLOBAL_APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  private void updateNexusDockerArtifactStreamAndValidate(NexusArtifactStream nexusDockerArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(nexusDockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-private/wingsplugings/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker-private/wingsplugings/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(NexusArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("docker-private");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugings/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getNexusDockerRegistryUrl())
        .isEqualTo("https://nexus3.harness.io");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactName()).isEmpty();

    NexusArtifactStream savedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(savedNexusArtifactStream.getJobname()).isEqualTo("docker-private");
    assertThat(savedNexusArtifactStream.getGroupId()).isEqualTo("wingsplugings/todolist");
    assertThat(savedNexusArtifactStream.getImageName()).isEqualTo("wingsplugings/todolist");
    assertThat(savedNexusArtifactStream.getRepositoryType()).isEqualTo("docker");

    savedNexusArtifactStream.setName("Nexus_Changed");
    savedNexusArtifactStream.setJobname("docker-hub");
    savedNexusArtifactStream.setGroupId("wingsplugings/todolist-changed");
    savedNexusArtifactStream.setImageName("wingsplugings/todolist-changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedNexusArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Nexus_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-hub/wingsplugings/todolist-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("docker-hub/wingsplugings/todolist-changed");
    assertThat(updatedArtifactStream).isInstanceOf(NexusArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(NEXUS.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("docker-hub");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getGroupId())
        .isEqualTo("wingsplugings/todolist-changed");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getImageName())
        .isEqualTo("wingsplugings/todolist-changed");

    NexusArtifactStream updatedNexusArtifactStream = (NexusArtifactStream) savedArtifactSteam;
    assertThat(updatedNexusArtifactStream.getJobname()).isEqualTo("docker-hub");
    assertThat(updatedNexusArtifactStream.getImageName()).isEqualTo("wingsplugings/todolist-changed");
    assertThat(updatedNexusArtifactStream.getRepositoryType()).isEqualTo("docker");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddArtifactoryArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    createAndValidateArtifactoryArtifactStream(artifactoryArtifactStream, APP_ID, "any");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddArtifactoryArtifactStreamAtConnectorLevel() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(GLOBAL_APP_ID)
                                                              .repositoryType(RepositoryType.any.name())
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .build();
    createAndValidateArtifactoryArtifactStream(artifactoryArtifactStream, GLOBAL_APP_ID, RepositoryType.any.name());
  }

  private void createAndValidateArtifactoryArtifactStream(
      ArtifactoryArtifactStream artifactoryArtifactStream, String appId, String repositoryType) {
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo(repositoryType);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("generic-repo");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("generic-repo");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern()).isEqualTo("io/harness/todolist/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo(repositoryType);

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
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
    updateArtifactoryArtifactStreamAndValidate(artifactoryArtifactStream, APP_ID, "any");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateArtifactoryArtifactStreamAtConnectorLevel() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(GLOBAL_APP_ID)
                                                              .repositoryType(RepositoryType.any.name())
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .build();
    updateArtifactoryArtifactStreamAndValidate(artifactoryArtifactStream, GLOBAL_APP_ID, RepositoryType.any.name());
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void shouldNotUpdateArtifactoryArtifactStreamAtConnectorLevelWithDifferentRepositoryType() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(GLOBAL_APP_ID)
                                                              .repositoryType(RepositoryType.any.name())
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .build();
    ArtifactoryArtifactStream savedArtifactSteam =
        (ArtifactoryArtifactStream) artifactStreamService.create(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getRepositoryType()).isEqualTo(RepositoryType.any.name());
    savedArtifactSteam.setRepositoryType(RepositoryType.docker.name());
    artifactStreamService.update(savedArtifactSteam);
  }

  private void updateArtifactoryArtifactStreamAndValidate(
      ArtifactoryArtifactStream artifactoryArtifactStream, String appId, String repositoryType) {
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("generic-repo/io/harness/todolist/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo(repositoryType);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("generic-repo");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("generic-repo");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern()).isEqualTo("io/harness/todolist/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo(repositoryType);

    savedArtifactoryArtifactStream.setName("Aritfactory_Changed");
    savedArtifactoryArtifactStream.setJobname("harness-rpm");
    savedArtifactoryArtifactStream.setArtifactPattern("todolist*");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedArtifactoryArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isEqualTo("Aritfactory_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("harness-rpm/todolist*");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harness-rpm/todolist*");
    assertThat(updatedArtifactStream).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo(repositoryType);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harness-rpm");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactPattern()).isEqualTo("todolist*");

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-rpm");
    assertThat(updatedArtifactoryArtifactStream.getArtifactPattern()).isEqualTo("todolist*");
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType()).isEqualTo(repositoryType);

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddArtifactoryMavenArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream =
        ArtifactoryArtifactStream.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .repositoryType("maven")
            .settingId(SETTING_ID)
            .jobname("harness-maven")
            .artifactPattern("io/harness/todolist/todolist/*/todolist*")
            .autoPopulate(true)
            .serviceId(SERVICE_ID)
            .build();
    addArtifactoryMavenArtifactStreamAndValidate(artifactoryArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddArtifactoryMavenArtifactStreamAtConnectorLevel() {
    ArtifactoryArtifactStream artifactoryArtifactStream =
        ArtifactoryArtifactStream.builder()
            .accountId(ACCOUNT_ID)
            .appId(GLOBAL_APP_ID)
            .repositoryType("maven")
            .settingId(SETTING_ID)
            .jobname("harness-maven")
            .artifactPattern("io/harness/todolist/todolist/*/todolist*")
            .autoPopulate(true)
            .build();
    addArtifactoryMavenArtifactStreamAndValidate(artifactoryArtifactStream, GLOBAL_APP_ID);
  }

  private void addArtifactoryMavenArtifactStreamAndValidate(
      ArtifactoryArtifactStream artifactoryArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo("maven");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harness-maven");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist*");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("harness-maven");
    assertThat(savedArtifactoryArtifactStream.getArtifactPattern())
        .isEqualTo("io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("maven");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateArtifactoryMavenArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream =
        ArtifactoryArtifactStream.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .repositoryType("any")
            .settingId(SETTING_ID)
            .jobname("harness-maven")
            .artifactPattern("io/harness/todolist/todolist/*/todolist*")
            .autoPopulate(true)
            .serviceId(SERVICE_ID)
            .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(APP_ID);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harness-maven/io/harness/todolist/todolist/*/todolist*");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harness-maven");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactPattern())
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
    assertThat(updatedArtifactStream.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("io/harness/todolist/todolist/*/todolist2*");
    assertThat(updatedArtifactStream.getSourceName())
        .isEqualTo("harness-maven2/io/harness/todolist/todolist/*/todolist2*");
    assertThat(updatedArtifactStream).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo("any");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harness-maven2");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactPattern())
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
  @Category(UnitTests.class)
  public void shouldAddArtifactoryDockerArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .jobname("docker")
                                                              .groupId("wingsplugins/todolist")
                                                              .imageName("wingsplugins/todolist")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    addArtifactoryDockerArtifactStreamAndValidate(artifactoryArtifactStream, APP_ID);
  }

  private void addArtifactoryDockerArtifactStreamAndValidate(
      ArtifactoryArtifactStream artifactoryArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo("docker");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("docker");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugins/todolist");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("docker");
    assertThat(savedArtifactoryArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("docker");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateArtifactoryDockerArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .jobname("docker")
                                                              .groupId("wingsplugins/todolist")
                                                              .imageName("wingsplugins/todolist")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    updateArtifactoryDockerArtifactStreamAndValidate(artifactoryArtifactStream, APP_ID);
  }

  private void updateArtifactoryDockerArtifactStreamAndValidate(
      ArtifactoryArtifactStream artifactoryArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("docker/wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo("docker");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("docker");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getGroupId()).isEqualTo("wingsplugins/todolist");

    ArtifactoryArtifactStream savedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(savedArtifactoryArtifactStream.getJobname()).isEqualTo("docker");
    assertThat(savedArtifactoryArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("docker");

    savedArtifactoryArtifactStream.setName("Aritfactory_Changed");
    savedArtifactoryArtifactStream.setJobname("docker-local");
    //    savedArtifactoryArtifactStream.setArtifactPattern("todolist*");
    savedArtifactoryArtifactStream.setGroupId("wingsplugins/todolist-changed");
    savedArtifactoryArtifactStream.setImageName("wingsplugins/todolist-changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedArtifactoryArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isEqualTo("Aritfactory_Changed");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("docker-local/wingsplugins/todolist-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("docker-local/wingsplugins/todolist-changed");
    assertThat(updatedArtifactStream).isInstanceOf(ArtifactoryArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ARTIFACTORY.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getRepositoryType()).isEqualTo("docker");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("docker-local");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName())
        .isEqualTo("wingsplugins/todolist-changed");

    ArtifactoryArtifactStream updatedArtifactoryArtifactStream = (ArtifactoryArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactoryArtifactStream.getJobname()).isEqualTo("docker-local");
    assertThat(updatedArtifactoryArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist-changed");
    assertThat(updatedArtifactoryArtifactStream.getRepositoryType()).isEqualTo("docker");

    verify(appService, times(2)).getAccountIdByAppId(appId);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddAmiArtifactStream() {
    AmiArtifactStream.Tag tag = new AmiArtifactStream.Tag();
    tag.setKey("name");
    tag.setValue("jenkins");
    AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .region("us-east-1")
                                              .tags(asList(tag))
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    validateAmiArtifactStream(amiArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddAmiArtifactStreamAtCloudProviderLevel() {
    AmiArtifactStream.Tag tag = new AmiArtifactStream.Tag();
    tag.setKey("name");
    tag.setValue("jenkins");
    AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .settingId(SETTING_ID)
                                              .region("us-east-1")
                                              .tags(asList(tag))
                                              .autoPopulate(true)
                                              .build();
    validateAmiArtifactStream(amiArtifactStream, GLOBAL_APP_ID);
  }

  private void validateAmiArtifactStream(AmiArtifactStream amiArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(amiArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam).isInstanceOf(AmiArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    AmiArtifactStream savedAmiArtifactStream = (AmiArtifactStream) savedArtifactSteam;
    assertThat(savedAmiArtifactStream.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateAmiArtifactStream() {
    AmiArtifactStream.Tag tag = new AmiArtifactStream.Tag();
    tag.setKey("name");
    tag.setValue("jenkins");

    AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .region("us-east-1")
                                              .tags(asList(tag))
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    updateAmiArtifactStreamAndValidate(amiArtifactStream, APP_ID);

    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateAmiArtifactStreamAtCloudProviderLevel() {
    AmiArtifactStream.Tag tag = new AmiArtifactStream.Tag();
    tag.setKey("name");
    tag.setValue("jenkins");

    AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .settingId(SETTING_ID)
                                              .region("us-east-1")
                                              .tags(asList(tag))
                                              .autoPopulate(true)
                                              .build();
    updateAmiArtifactStreamAndValidate(amiArtifactStream, GLOBAL_APP_ID);

    verify(yamlPushService, times(2))
        .pushYamlChangeSet(
            any(String.class), any(ArtifactStream.class), any(ArtifactStream.class), any(), anyBoolean(), anyBoolean());
  }

  private void updateAmiArtifactStreamAndValidate(AmiArtifactStream amiArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(amiArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("us-east-1:name:jenkins");
    assertThat(savedArtifactSteam).isInstanceOf(AmiArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getTags()).containsKey("name");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getTags()).containsValue(asList("jenkins"));

    AmiArtifactStream savedAmiArtifactStream = (AmiArtifactStream) savedArtifactSteam;
    assertThat(savedAmiArtifactStream.getRegion()).isEqualTo("us-east-1");

    AmiArtifactStream.Tag updatedTag = new AmiArtifactStream.Tag();
    updatedTag.setKey("name2");
    updatedTag.setValue("jenkins2");
    savedAmiArtifactStream.getTags().add(updatedTag);
    savedAmiArtifactStream.setRegion("us-west");

    ArtifactStream updatedAmiArtifactStream = artifactStreamService.update(savedAmiArtifactStream);
    assertThat(updatedAmiArtifactStream.getAccountId()).isEqualTo(ACCOUNT_ID);

    assertThat(updatedAmiArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedAmiArtifactStream.getName()).isNotEmpty();
    assertThat(updatedAmiArtifactStream.getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(updatedAmiArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedAmiArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("us-west:name:jenkins");
    assertThat(updatedAmiArtifactStream.getSourceName()).isEqualTo("us-west:name:jenkins_name2:jenkins2");
    assertThat(updatedAmiArtifactStream).isInstanceOf(AmiArtifactStream.class);
    assertThat(updatedAmiArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMI.name());
    assertThat(updatedAmiArtifactStream.fetchArtifactStreamAttributes().getRegion()).isEqualTo("us-west");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getTags()).containsKey("name");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getTags()).containsValue(asList("jenkins"));
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getTags()).containsKey("name2");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getTags()).containsValue(asList("jenkins2"));

    AmiArtifactStream updatedArtifactStream = (AmiArtifactStream) savedArtifactSteam;
    assertThat(updatedArtifactStream.getRegion()).isEqualTo("us-west");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddS3ArtifactStream() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();
    validateS3ArtifactStream(amazonS3ArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddS3ArtifactStreamAtConnectorLevel() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(GLOBAL_APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .autoPopulate(true)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();
    validateS3ArtifactStream(amazonS3ArtifactStream, GLOBAL_APP_ID);
  }

  private void validateS3ArtifactStream(AmazonS3ArtifactStream amazonS3ArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(amazonS3ArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("harnessapps");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessapps/dev/todolist.war");
    assertThat(savedArtifactSteam).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harnessapps");
    AmazonS3ArtifactStream savedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(savedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps");
    assertThat(savedAmazonS3ArtifactStream.getArtifactPaths()).contains("dev/todolist.war");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateS3ArtifactStream() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .autoPopulate(true)
                                                        .serviceId(SERVICE_ID)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();
    updateS3ArtifactStreamAndValidate(amazonS3ArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateS3ArtifactStreamAtConnectorLevel() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(GLOBAL_APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .autoPopulate(true)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();
    updateS3ArtifactStreamAndValidate(amazonS3ArtifactStream, GLOBAL_APP_ID);
  }

  private void updateS3ArtifactStreamAndValidate(AmazonS3ArtifactStream amazonS3ArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(amazonS3ArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("harnessapps");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessapps/dev/todolist.war");
    assertThat(savedArtifactSteam).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harnessapps");
    AmazonS3ArtifactStream savedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(savedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps");
    assertThat(savedAmazonS3ArtifactStream.getArtifactPaths()).contains("dev/todolist.war");

    savedAmazonS3ArtifactStream.setJobname("harnessapps-changed");
    savedAmazonS3ArtifactStream.setName("s3 stream");
    savedAmazonS3ArtifactStream.setArtifactPaths(asList("qa/todolist.war"));

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedAmazonS3ArtifactStream);
    assertThat(updatedArtifactStream.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("s3 stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(AMAZON_S3.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("harnessapps-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harnessapps-changed/qa/todolist.war");
    assertThat(updatedArtifactStream).isInstanceOf(AmazonS3ArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(AMAZON_S3.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getJobName()).isEqualTo("harnessapps-changed");
    AmazonS3ArtifactStream updatedAmazonS3ArtifactStream = (AmazonS3ArtifactStream) savedArtifactSteam;
    assertThat(updatedAmazonS3ArtifactStream.getJobname()).isEqualTo("harnessapps-changed");
    assertThat(updatedAmazonS3ArtifactStream.getArtifactPaths()).contains("qa/todolist.war");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddDockerArtifactStream() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    validateDockerArtifactStream(dockerArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddDockerArtifactStreamAtConnectorLevel() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(GLOBAL_APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .build();
    validateDockerArtifactStream(dockerArtifactStream, GLOBAL_APP_ID);
  }

  private void validateDockerArtifactStream(DockerArtifactStream dockerArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(dockerArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(DockerArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
    DockerArtifactStream savedDockerArtifactStream = (DockerArtifactStream) savedArtifactSteam;
    assertThat(savedDockerArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateDockerArtifactStream() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    updateAndValidateDockerArtifactStream(dockerArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateDockerArtifactStreamAtConnectorLevel() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(GLOBAL_APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .build();
    updateAndValidateDockerArtifactStream(dockerArtifactStream, GLOBAL_APP_ID);
  }

  private void updateAndValidateDockerArtifactStream(DockerArtifactStream dockerArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("wingsplugins/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("wingsplugins/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(DockerArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("wingsplugins/todolist");
    DockerArtifactStream savedDockerArtifactStream = (DockerArtifactStream) savedArtifactSteam;
    assertThat(savedDockerArtifactStream.getImageName()).isEqualTo("wingsplugins/todolist");

    savedDockerArtifactStream.setImageName("harness/todolist");
    savedArtifactSteam.setName("Docker Stream");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedDockerArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Docker Stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("harness/todolist");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harness/todolist");
    assertThat(updatedArtifactStream).isInstanceOf(DockerArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(DOCKER.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getImageName()).isEqualTo("harness/todolist");
    DockerArtifactStream updatedDockerArtifactStream = (DockerArtifactStream) savedArtifactSteam;
    assertThat(updatedDockerArtifactStream.getImageName()).isEqualTo("harness/todolist");

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddEcrArtifactStream() {
    EcrArtifactStream dockerArtifactStream = EcrArtifactStream.builder()
                                                 .accountId(ACCOUNT_ID)
                                                 .appId(APP_ID)
                                                 .settingId(SETTING_ID)
                                                 .imageName("todolist")
                                                 .region("us-east-1")
                                                 .autoPopulate(true)
                                                 .serviceId(SERVICE_ID)
                                                 .build();
    validateECRArtifactStream(dockerArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddEcrArtifactStreamAtCloudProviderLevel() {
    EcrArtifactStream dockerArtifactStream = EcrArtifactStream.builder()
                                                 .accountId(ACCOUNT_ID)
                                                 .appId(GLOBAL_APP_ID)
                                                 .settingId(SETTING_ID)
                                                 .imageName("todolist")
                                                 .region("us-east-1")
                                                 .autoPopulate(true)
                                                 .build();
    validateECRArtifactStream(dockerArtifactStream, GLOBAL_APP_ID);
  }

  private void validateECRArtifactStream(EcrArtifactStream dockerArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(dockerArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolist");
    assertThat(savedArtifactSteam).isInstanceOf(EcrArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    EcrArtifactStream savedEcrArtifactStream = (EcrArtifactStream) savedArtifactSteam;
    assertThat(savedEcrArtifactStream.getImageName()).isEqualTo("todolist");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateEcrArtifactStream() {
    EcrArtifactStream dockerArtifactStream = EcrArtifactStream.builder()
                                                 .accountId(ACCOUNT_ID)
                                                 .appId(APP_ID)
                                                 .settingId(SETTING_ID)
                                                 .imageName("todolist")
                                                 .region("us-east-1")
                                                 .autoPopulate(true)
                                                 .serviceId(SERVICE_ID)
                                                 .build();
    updateAndValidateECRArtifactStream(dockerArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateEcrArtifactStreamAtCloudProviderLevel() {
    EcrArtifactStream dockerArtifactStream = EcrArtifactStream.builder()
                                                 .accountId(ACCOUNT_ID)
                                                 .appId(GLOBAL_APP_ID)
                                                 .settingId(SETTING_ID)
                                                 .imageName("todolist")
                                                 .region("us-east-1")
                                                 .autoPopulate(true)
                                                 .build();
    updateAndValidateECRArtifactStream(dockerArtifactStream, GLOBAL_APP_ID);
  }

  private void updateAndValidateECRArtifactStream(EcrArtifactStream dockerArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("todolist");
    assertThat(savedArtifactSteam).isInstanceOf(EcrArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName()).isEqualTo("todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegion()).isEqualTo("us-east-1");
    EcrArtifactStream savedEcrArtifactStream = (EcrArtifactStream) savedArtifactSteam;

    savedEcrArtifactStream.setRegion("us-west");
    savedEcrArtifactStream.setName("Ecr Stream");
    savedEcrArtifactStream.setImageName("todolist-changed");

    ArtifactStream updatedArtifactStream = artifactStreamService.update(savedEcrArtifactStream);
    assertThat(updatedArtifactStream.getUuid()).isNotEmpty();
    assertThat(updatedArtifactStream.getName()).isNotEmpty().isEqualTo("Ecr Stream");
    assertThat(updatedArtifactStream.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("todolist-changed");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("todolist-changed");

    assertThat(updatedArtifactStream).isInstanceOf(EcrArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getImageName()).isEqualTo("todolist-changed");
    EcrArtifactStream updatedEcrArtifactStream = (EcrArtifactStream) savedArtifactSteam;
    assertThat(updatedEcrArtifactStream.getImageName()).isEqualTo("todolist-changed");

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddGcrArtifactStream() {
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    validateGCRArtifactStream(gcrArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddGcrArtifactStreamAtCloudProviderLevel() {
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .build();
    validateGCRArtifactStream(gcrArtifactStream, GLOBAL_APP_ID);
  }

  private void validateGCRArtifactStream(GcrArtifactStream gcrArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(gcrArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("exploration-161417/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("gcr.io/exploration-161417/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(GcrArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName())
        .isEqualTo("exploration-161417/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegistryHostName()).isEqualTo("gcr.io");
    GcrArtifactStream savedGcrArtifactStream = (GcrArtifactStream) savedArtifactSteam;
    assertThat(savedGcrArtifactStream.getDockerImageName()).isEqualTo("exploration-161417/todolist");
    assertThat(savedGcrArtifactStream.getRegistryHostName()).isEqualTo("gcr.io");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateGcrArtifactStream() {
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    updateAndValidateGCRArtifactStream(gcrArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateGcrArtifactStreamAtCloudProviderLevel() {
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .build();
    updateAndValidateGCRArtifactStream(gcrArtifactStream, GLOBAL_APP_ID);
  }

  private void updateAndValidateGCRArtifactStream(GcrArtifactStream gcrArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(gcrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("exploration-161417/todolist");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("gcr.io/exploration-161417/todolist");
    assertThat(savedArtifactSteam).isInstanceOf(GcrArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getImageName())
        .isEqualTo("exploration-161417/todolist");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegistryHostName()).isEqualTo("gcr.io");
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
    assertThat(updatedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactSteam.fetchArtifactDisplayName(""))
        .isNotEmpty()
        .contains("exploration-161417/todolist-changed");
    assertThat(updatedArtifactSteam.getSourceName()).isEqualTo("gcr.io/exploration-161417/todolist-changed");
    assertThat(updatedArtifactSteam).isInstanceOf(GcrArtifactStream.class);
    assertThat(updatedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType()).isEqualTo(GCR.name());
    assertThat(updatedArtifactSteam.fetchArtifactStreamAttributes().getImageName())
        .isEqualTo("exploration-161417/todolist-changed");
    assertThat(updatedArtifactSteam.fetchArtifactStreamAttributes().getRegistryHostName()).isEqualTo("gcr.io");

    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddAcrArtifactStream() {
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    validateAcrArtifactStream(acrArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAddAcrArtifactStreamAtCloudProviderLevel() {
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .build();
    validateAcrArtifactStream(acrArtifactStream, GLOBAL_APP_ID);
  }

  private void validateAcrArtifactStream(AcrArtifactStream acrArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(acrArtifactStream);
    assertThat(savedArtifactSteam.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("harnessqa/nginx");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessqa/nginx");
    assertThat(savedArtifactSteam).isInstanceOf(AcrArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getSubscriptionId())
        .isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryName()).isEqualTo("nginx");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegistryName()).isEqualTo("harnessqa");
    AcrArtifactStream savedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(savedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedAcrArtifactStream.getRepositoryName()).isEqualTo("nginx");
    assertThat(savedAcrArtifactStream.getRegistryName()).isEqualTo("harnessqa");

    verify(buildSourceService).validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateAcrArtifactStream() {
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    updateAndValidateACRArtifactStream(acrArtifactStream, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateAcrArtifactStreamAtCloudProviderLevel() {
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .build();
    updateAndValidateACRArtifactStream(acrArtifactStream, GLOBAL_APP_ID);
  }

  private void updateAndValidateACRArtifactStream(AcrArtifactStream acrArtifactStream, String appId) {
    ArtifactStream savedArtifactSteam = createArtifactStream(acrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.getAppId()).isEqualTo(appId);
    assertThat(savedArtifactSteam.fetchArtifactDisplayName("")).isNotEmpty().contains("harnessqa/nginx");
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo("harnessqa/nginx");
    assertThat(savedArtifactSteam).isInstanceOf(AcrArtifactStream.class);
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getSubscriptionId())
        .isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRepositoryName()).isEqualTo("nginx");
    assertThat(savedArtifactSteam.fetchArtifactStreamAttributes().getRegistryName()).isEqualTo("harnessqa");
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
    assertThat(updatedArtifactStream.getAppId()).isEqualTo(appId);
    assertThat(updatedArtifactStream.fetchArtifactDisplayName("")).isNotEmpty().contains("harnessprod/istio");
    assertThat(updatedArtifactStream.getSourceName()).isEqualTo("harnessprod/istio");
    assertThat(updatedArtifactStream).isInstanceOf(AcrArtifactStream.class);
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getArtifactStreamType())
        .isEqualTo(ArtifactStreamType.ACR.name());
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getSubscriptionId())
        .isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getRepositoryName()).isEqualTo("istio");
    assertThat(updatedArtifactStream.fetchArtifactStreamAttributes().getRegistryName()).isEqualTo("harnessprod");

    AcrArtifactStream updatedAcrArtifactStream = (AcrArtifactStream) savedArtifactSteam;
    assertThat(updatedAcrArtifactStream.getSubscriptionId()).isEqualTo("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0");
    assertThat(updatedAcrArtifactStream.getRepositoryName()).isEqualTo("istio");
    assertThat(updatedAcrArtifactStream.getRegistryName()).isEqualTo("harnessprod");
    verify(buildSourceService, times(2))
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListArtifactStreams() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedJenkinsArtifactSteam = createArtifactStream(jenkinsArtifactStream);

    assertThat(savedJenkinsArtifactSteam).isNotNull();

    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    List<String> artifactStreamIds = asList(savedJenkinsArtifactSteam.getUuid(), savedArtifactStream.getUuid());
    Service service = Service.builder().artifactStreamIds(artifactStreamIds).build();
    when(serviceResourceService.findServicesByApp(APP_ID)).thenReturn(Collections.singletonList(service));
    when(artifactStreamServiceBindingService.listArtifactStreamIds(service)).thenReturn(artifactStreamIds);

    List<ArtifactStream> artifactStreams = artifactStreamService.listByAppId(APP_ID);
    assertThat(artifactStreams).isNotEmpty().size().isEqualTo(2);
    assertThat(artifactStreams)
        .extracting(artifactStream -> artifactStream.getArtifactStreamType())
        .contains(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.ARTIFACTORY.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListArtifactStreamsAtConnectorLevel() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStreamAtConnectorLevel();
    ArtifactStream savedJenkinsArtifactStream = createArtifactStream(jenkinsArtifactStream);

    assertThat(savedJenkinsArtifactStream).isNotNull();

    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(GLOBAL_APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(ANOTHER_SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .build();

    ArtifactStream savedArtifactStream = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    List<ArtifactStream> artifactStreams = artifactStreamService.listBySettingId(SETTING_ID);
    assertThat(artifactStreams).isNotEmpty().size().isEqualTo(1);
    assertThat(artifactStreams)
        .extracting(artifactStream -> artifactStream.getArtifactStreamType())
        .contains(ArtifactStreamType.JENKINS.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    ArtifactStream artifactStream = artifactStreamService.get(savedArtifactStream.getUuid());
    assertThat(artifactStream.getUuid()).isEqualTo(savedArtifactStream.getUuid());
    assertThat(artifactStream.getName()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactStreamAtConnectorLevel() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStreamAtConnectorLevel();
    ArtifactStream savedArtifactStream = createArtifactStream(jenkinsArtifactStream);
    assertThat(savedArtifactStream).isNotNull();
    assertThat(savedArtifactStream.getAppId()).isEqualTo(GLOBAL_APP_ID);
    ArtifactStream artifactStream = artifactStreamService.get(savedArtifactStream.getUuid());
    assertThat(artifactStream.getUuid()).isEqualTo(savedArtifactStream.getUuid());
    assertThat(artifactStream.getName()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteArtifactStreamAtConnectorLevel() {
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStreamAtConnectorLevel();
    ArtifactStream savedArtifactStream = createArtifactStream(jenkinsArtifactStream);
    assertThat(savedArtifactStream).isNotNull();
    assertThat(artifactStreamService.delete(savedArtifactStream.getUuid(), false)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();
    assertThat(artifactStreamService.delete(APP_ID, savedArtifactStream.getUuid())).isTrue();
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void shouldNotDeleteArtifactStream() {
    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();

    ArtifactStream savedArtifactStream = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactStream).isNotNull();

    when(triggerService.getTriggersHasArtifactStreamAction(APP_ID, savedArtifactStream.getUuid()))
        .thenReturn(
            Collections.singletonList(software.wings.beans.trigger.Trigger.builder().name(TRIGGER_NAME).build()));
    assertThat(artifactStreamService.delete(APP_ID, savedArtifactStream.getUuid())).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDockerArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(DockerConfig.builder()
                        .dockerRegistryUrl("http://hub.docker.com/")
                        .username("username")
                        .password("password".toCharArray())
                        .accountId(ACCOUNT_ID)
                        .build());
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(dockerArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getName()).isNotEmpty();
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(DOCKER.name());
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY,
            ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://hub.docker.com/", "wingsplugins/todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDockerArtifactSourcePropertiesWhenArtifactStreamDeleted() {
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, ARTIFACT_STREAM_ID);
    assertThat(artifactSourceProperties).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetGcrArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(GcpConfig.builder().accountId(ACCOUNT_ID).build());
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .dockerImageName("exploration-161417/todolist")
                                              .registryHostName("gcr.io")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(gcrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY,
            ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("gcr.io", "exploration-161417/todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAcrArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(AzureConfig.builder().accountId(ACCOUNT_ID).build());

    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .subscriptionId("20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0")
                                              .repositoryName("nginx")
                                              .registryName("harnessqa")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(acrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_REPOSITORY_NAME_KEY,
            ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("harnessqa", "nginx");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetEcrArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(AzureConfig.builder().accountId(ACCOUNT_ID).build());

    EcrArtifactStream ecrArtifactStream = EcrArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .settingId(SETTING_ID)
                                              .imageName("todolist")
                                              .region("us-east-1")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(ecrArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetJenkinsArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(JenkinsConfig.builder()
                        .jenkinsUrl("http://jenkins.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsStream();
    ArtifactStream savedArtifactSteam = createArtifactStream(jenkinsArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://jenkins.software");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetBabmooArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(JenkinsConfig.builder()
                        .jenkinsUrl("http://bamboo.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .jobname("TOD-TOD")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .artifactPaths(asList("artifacts/todolist.war"))
                                                    .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(bambooArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://bamboo.software");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetNexusArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://bamboo.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .groupId("io.harness.test")
                                                  .artifactPaths(asList("todolist"))
                                                  .autoPopulate(true)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://bamboo.software");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetNexusDockerArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://bamboo.software")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("releases")
                                                  .imageName("wingsplugins/todolist")
                                                  .autoPopulate(true)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(nexusArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY,
            ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://bamboo.software", "wingsplugins/todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactoryArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://artifactory.com")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    ArtifactoryArtifactStream artifactoryArtifactStream = ArtifactoryArtifactStream.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .repositoryType("any")
                                                              .settingId(SETTING_ID)
                                                              .jobname("generic-repo")
                                                              .artifactPattern("io/harness/todolist/todolist*")
                                                              .autoPopulate(true)
                                                              .serviceId(SERVICE_ID)
                                                              .build();
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(
            ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://artifactory.com");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactoryDockerArtifactStreamSourceProperties() {
    when(settingsService.getSettingValueById(ACCOUNT_ID, SETTING_ID))
        .thenReturn(NexusConfig.builder()
                        .nexusUrl("http://artifactory.com")
                        .username("username")
                        .accountId(ACCOUNT_ID)
                        .build());

    ArtifactoryArtifactStream artifactoryArtifactStream = buildArtifactoryStream();
    ArtifactStream savedArtifactSteam = createArtifactStream(artifactoryArtifactStream);
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();

    Map<String, String> artifactSourceProperties =
        artifactStreamService.fetchArtifactSourceProperties(ACCOUNT_ID, savedArtifactSteam.getUuid());
    assertThat(artifactSourceProperties).isNotEmpty();
    assertThat(artifactSourceProperties)
        .containsOnlyKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY,
            ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY);
    assertThat(artifactSourceProperties).containsValues("username", "http://artifactory.com", "wingsplugins/todolist");
  }

  private ArtifactoryArtifactStream buildArtifactoryStream() {
    return ArtifactoryArtifactStream.builder()
        .accountId(ACCOUNT_ID)
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
  @Category(UnitTests.class)
  public void shouldListArtifactStreamIdsofService() {
    ArtifactStream savedArtifactSteam = createArtifactStream(buildArtifactoryStream());
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(artifactStreamService.fetchArtifactStreamIdsForService(APP_ID, SERVICE_ID))
        .isNotEmpty()
        .contains(savedArtifactSteam.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListArtifactStreamsofService() {
    ArtifactStream savedArtifactSteam = createArtifactStream(buildArtifactoryStream());
    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(artifactStreamService.fetchArtifactStreamsForService(APP_ID, SERVICE_ID))
        .isNotEmpty()
        .extracting(ArtifactStream::getUuid)
        .isNotNull()
        .contains(savedArtifactSteam.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCRUDCustomArtifactStream() {
    ArtifactStream customArtifactStream =
        CustomArtifactStream.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .serviceId(SERVICE_ID)
            .name("Custom Artifact Stream" + System.currentTimeMillis())
            .scripts(Arrays.asList(CustomArtifactStream.Script.builder()
                                       .action(Action.FETCH_VERSIONS)
                                       .scriptString("echo Hello World!! and echo ${secrets.getValue(My Secret)}")
                                       .build()))
            .build();

    ArtifactStream savedArtifactSteam = createArtifactStream(customArtifactStream);

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

    assertThat(artifactStreamService.get(updatedArtifactStream.getUuid())).isNull();

    verify(artifactService, times(0)).deleteWhenArtifactSourceNameChanged(customArtifactStream);
    verify(triggerService).updateByArtifactStream(updatedArtifactStream.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCRUDCustomArtifactStreamWithCustomMapping() {
    List<CustomRepositoryMapping.AttributeMapping> attributeMapping = new ArrayList<>();
    attributeMapping.add(builder().relativePath("assets.downloadUrl").mappedAttribute("metadata.downloadUrl").build());
    CustomRepositoryMapping mapping = CustomRepositoryMapping.builder()
                                          .artifactRoot("$.items")
                                          .buildNoPath("version")
                                          .artifactAttributes(attributeMapping)
                                          .build();
    ArtifactStream customArtifactStream =
        CustomArtifactStream.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .serviceId(SERVICE_ID)
            .name("Custom Artifact Stream" + System.currentTimeMillis())
            .scripts(Arrays.asList(CustomArtifactStream.Script.builder()
                                       .action(Action.FETCH_VERSIONS)
                                       .scriptString("echo Hello World!! and echo ${secrets.getValue(My Secret)}")
                                       .customRepositoryMapping(mapping)
                                       .build()))
            .build();

    ArtifactStream savedArtifactSteam = createArtifactStream(customArtifactStream);

    assertThat(savedArtifactSteam.getUuid()).isNotEmpty();
    assertThat(savedArtifactSteam.getSourceName()).isEqualTo(savedArtifactSteam.getName());
    assertThat(savedArtifactSteam.getArtifactStreamType()).isEqualTo(ArtifactStreamType.CUSTOM.name());
    CustomArtifactStream savedCustomArtifactStream = (CustomArtifactStream) savedArtifactSteam;
    assertThat(savedCustomArtifactStream.getScripts()).isNotEmpty();
    Script script = savedCustomArtifactStream.getScripts().stream().findFirst().orElse(null);
    assertThat(script.getAction()).isEqualTo(Action.FETCH_VERSIONS);
    assertThat(script.getScriptString()).isEqualTo("echo Hello World!! and echo ${secrets.getValue(My Secret)}");
    assertThat(script.getCustomRepositoryMapping()).isNotNull();
    assertThat(script.getCustomRepositoryMapping().getBuildNoPath()).isEqualTo("version");
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes().size()).isEqualTo(1);
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes())
        .extracting("mappedAttribute")
        .contains("metadata.downloadUrl");

    assertThat(artifactStreamService.fetchArtifactStreamsForService(APP_ID, SERVICE_ID))
        .isNotEmpty()
        .extracting(ArtifactStream::getUuid)
        .isNotNull()
        .contains(savedArtifactSteam.getUuid());

    attributeMapping = new ArrayList<>();
    attributeMapping.add(builder().relativePath("assets.path").mappedAttribute("metadata.path").build());
    mapping = CustomRepositoryMapping.builder()
                  .artifactRoot("$.items")
                  .buildNoPath("version")
                  .artifactAttributes(attributeMapping)
                  .build();
    script.setCustomRepositoryMapping(mapping);
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
    assertThat(script.getCustomRepositoryMapping()).isNotNull();
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes().size()).isEqualTo(1);
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes())
        .extracting("mappedAttribute")
        .contains("metadata.path");
    assertThat(script.getCustomRepositoryMapping().getArtifactAttributes())
        .extracting("mappedAttribute")
        .doesNotContain("metadata.downloadUrl");

    artifactStreamService.delete(APP_ID, updatedArtifactStream.getUuid());

    assertThat(artifactStreamService.get(updatedArtifactStream.getUuid())).isNull();

    verify(artifactService, times(0)).deleteWhenArtifactSourceNameChanged(customArtifactStream);
    verify(triggerService).updateByArtifactStream(updatedArtifactStream.getUuid());
  }
}
