package software.wings.integration.service.impl;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.Category.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.Category.CONNECTOR;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.HARNESS_ARTIFACTORY;
import static software.wings.utils.WingsTestConstants.HARNESS_BAMBOO;
import static software.wings.utils.WingsTestConstants.HARNESS_DOCKER_REGISTRY;
import static software.wings.utils.WingsTestConstants.HARNESS_NEXUS;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.ArtifactType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rsingh on 10/9/17.
 * Integration test that goes against a test instance of Jenkins (among other things) and verifies behaviours.
 */
@RunWith(Parameterized.class)
@Ignore // TODO: fix this test to use setting from settings generator
public class BuildSourceServiceIntegrationTest extends WingsBaseTest {
  public static final String TEST_JENKINS_URL = "http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/";
  @Parameter(0) public SettingVariableTypes type;
  @Parameter(1) public ArtifactStreamType streamType;
  @Parameter(2) public String repositoryType;
  @Parameter(3) public String jobName;
  @Parameter(4) public String groupId;
  @Parameter(5) public String artifactPath;
  @Parameter(6) public ArtifactType artifactType;

  private String accountId;
  private String appId;
  private SettingAttribute settingAttribute;
  private ArtifactStream artifactStream;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private BuildSourceService buildSourceService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private JenkinsBuildService jenkinsBuildService;
  @Inject private BambooBuildService bambooBuildService;
  @Inject private NexusBuildService nexusBuildService;
  @Inject private DockerBuildService dockerBuildService;
  @Inject private ArtifactoryBuildService artifactoryBuildService;
  @Inject private EcrBuildService ecrBuildService;
  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {SettingVariableTypes.JENKINS, ArtifactStreamType.JENKINS, "", "todolist_war", "", "target/todolist.war", WAR},

        {SettingVariableTypes.BAMBOO, ArtifactStreamType.BAMBOO, "", "TOD-TOD", "", "artifacts/todolist.war", WAR},

        {SettingVariableTypes.NEXUS, ArtifactStreamType.NEXUS, "", "releases", "io.harness.test", "todolist", WAR},
        {SettingVariableTypes.DOCKER, ArtifactStreamType.DOCKER, "", "xyz", "", "wingsplugins/todolist", WAR},
        {SettingVariableTypes.ARTIFACTORY, ArtifactStreamType.ARTIFACTORY, "any", "generic-repo",
            "io/harness/todolist/todolist*", "", WAR},
        {SettingVariableTypes.ARTIFACTORY, ArtifactStreamType.ARTIFACTORY, "any", "harness-rpm", "todolist*", "", RPM},
        {SettingVariableTypes.ARTIFACTORY, ArtifactStreamType.ARTIFACTORY, "maven", "harness-maven",
            "io/harness/todolist/todolist/*/todolist*", "", WAR},
        {SettingVariableTypes.ARTIFACTORY, ArtifactStreamType.ARTIFACTORY, "docker", "docker", "wingsplugins/todolist",
            "", DOCKER},
        {SettingVariableTypes.ECR, ArtifactStreamType.ECR, "docker", Regions.US_EAST_1.getName(), "todolist",
            "todolist", DOCKER}});
  }

  @Before
  public void setUp() {
    initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
    Service service = Service.builder().appId(appId).artifactType(artifactType).name("Some service").build();
    wingsPersistence.save(service);
    switch (type) {
      case JENKINS:
        when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(jenkinsBuildService);
        settingAttribute = aSettingAttribute()
                               .withName("harness")
                               .withCategory(CONNECTOR)
                               .withAccountId(accountId)
                               .withValue(JenkinsConfig.builder()
                                              .accountId(accountId)
                                              .jenkinsUrl(TEST_JENKINS_URL)
                                              .username("admin")
                                              .password("admin".toCharArray())
                                              .build())
                               .build();
        artifactStream = new JenkinsArtifactStream();
        ((JenkinsArtifactStream) artifactStream).setJobname(jobName);
        ((JenkinsArtifactStream) artifactStream).setArtifactPaths(Collections.singletonList(artifactPath));
        artifactStream.setServiceId(service.getUuid());
        artifactStream.setAppId(appId);
        break;
      case BAMBOO:
        when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(bambooBuildService);
        settingAttribute = aSettingAttribute()
                               .withName(HARNESS_BAMBOO)
                               .withCategory(CONNECTOR)
                               .withAccountId(accountId)
                               .withValue(BambooConfig.builder()
                                              .accountId(accountId)
                                              .bambooUrl("http://ec2-34-205-16-35.compute-1.amazonaws.com:8085/")
                                              .username("wingsbuild")
                                              .password("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray())
                                              .build())
                               .build();
        artifactStream = new BambooArtifactStream();
        ((BambooArtifactStream) artifactStream).setJobname(jobName);
        ((BambooArtifactStream) artifactStream).setArtifactPaths(Collections.singletonList(artifactPath));
        artifactStream.setServiceId(service.getUuid());
        artifactStream.setAppId(appId);
        break;

      case NEXUS:
        when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(nexusBuildService);
        settingAttribute = aSettingAttribute()
                               .withName(HARNESS_NEXUS)
                               .withCategory(CONNECTOR)
                               .withAccountId(accountId)
                               .withValue(NexusConfig.builder()
                                              .accountId(accountId)
                                              .nexusUrl("https://nexus.wings.software")
                                              .username("admin")
                                              .password("wings123!".toCharArray())
                                              .build())
                               .build();
        artifactStream = new NexusArtifactStream();
        ((NexusArtifactStream) artifactStream).setJobname(jobName);
        ((NexusArtifactStream) artifactStream).setArtifactPaths(Collections.singletonList(artifactPath));
        ((NexusArtifactStream) artifactStream).setGroupId(groupId);
        artifactStream.setServiceId(service.getUuid());
        artifactStream.setAppId(appId);
        break;

      case DOCKER:
        when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(dockerBuildService);
        settingAttribute = aSettingAttribute()
                               .withName(HARNESS_DOCKER_REGISTRY)
                               .withCategory(CONNECTOR)
                               .withAccountId(accountId)
                               .withValue(DockerConfig.builder()
                                              .accountId(accountId)
                                              .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
                                              .username("wingsplugins")
                                              .password("W!ngs@DockerHub".toCharArray())
                                              .build())
                               .build();
        artifactStream = new DockerArtifactStream();
        ((DockerArtifactStream) artifactStream).setImageName(artifactPath);
        artifactStream.setServiceId(service.getUuid());
        artifactStream.setAppId(appId);
        break;
      case ARTIFACTORY:
        when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(artifactoryBuildService);
        settingAttribute = aSettingAttribute()
                               .withName(HARNESS_ARTIFACTORY)
                               .withCategory(CONNECTOR)
                               .withAccountId(accountId)
                               .withValue(ArtifactoryConfig.builder()
                                              .accountId(accountId)
                                              .artifactoryUrl("https://harness.jfrog.io/harness")
                                              .username("admin")
                                              .password("harness123!".toCharArray())
                                              .build())
                               .build();
        artifactStream = new ArtifactoryArtifactStream();
        ((ArtifactoryArtifactStream) artifactStream).setJobname(jobName);
        ((ArtifactoryArtifactStream) artifactStream).setArtifactPattern(groupId);
        ((ArtifactoryArtifactStream) artifactStream).setRepositoryType(repositoryType);
        ((ArtifactoryArtifactStream) artifactStream).setImageName(groupId);
        artifactStream.setServiceId(service.getUuid());
        artifactStream.setAppId(appId);
        break;
      case ECR:
        when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(ecrBuildService);
        settingAttribute = aSettingAttribute()
                               .withName("AWS")
                               .withCategory(CLOUD_PROVIDER)
                               .withAccountId(accountId)
                               .withValue(AwsConfig.builder()
                                              .accountId(accountId)
                                              .accessKey("AKIAIKL7FYYF2TIYHCLQ")
                                              .secretKey("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".toCharArray())
                                              .build())
                               .build();
        artifactStream = new EcrArtifactStream();
        ((EcrArtifactStream) artifactStream).setRegion(Regions.US_EAST_1.getName());
        ((EcrArtifactStream) artifactStream).setImageName(groupId);
        artifactStream.setServiceId(service.getUuid());
        artifactStream.setAppId(appId);
        break;
      default:
        throw new IllegalArgumentException("Invalid type: " + type);
    }
    wingsPersistence.save(artifactStream);
    wingsPersistence.save(settingAttribute);
    setInternalState(buildSourceService, "delegateProxyFactory", delegateProxyFactory);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getJobs() {
    switch (type) {
      case DOCKER:
      case ARTIFACTORY:
      case ECR:
        return;

      default:
        Set<JobDetails> jobs = buildSourceService.getJobs(appId, settingAttribute.getUuid(), null);
        assertFalse(jobs.isEmpty());
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getPlans() {
    switch (type) {
      case DOCKER:
      case ECR:
        return;
      default:
        Map<String, String> plans = buildSourceService.getPlans(appId, settingAttribute.getUuid(), streamType.name());
        assertFalse(plans.isEmpty());
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getPlansWithService() {
    switch (type) {
      case DOCKER:
      case ECR:
        return;

      default:
        Service service = Service.builder().appId(appId).artifactType(WAR).name("Some service").build();
        wingsPersistence.save(service);
        Map<String, String> plans = buildSourceService.getPlans(
            appId, settingAttribute.getUuid(), service.getUuid(), streamType.name(), repositoryType);
        assertFalse(plans.isEmpty());
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getArtifactPaths() {
    switch (type) {
      case DOCKER:
      case ARTIFACTORY:
        return;

      default:
        Set<String> artifactPaths =
            buildSourceService.getArtifactPaths(appId, jobName, settingAttribute.getUuid(), groupId, streamType.name());
        assertFalse(artifactPaths.isEmpty());
        assertTrue(artifactPaths.contains(artifactPath));
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getBuilds() {
    switch (type) {
      case DOCKER:
      case ECR:
        return;

      default:
        List<BuildDetails> builds =
            buildSourceService.getBuilds(appId, artifactStream.getUuid(), settingAttribute.getUuid());
        assertFalse(builds.isEmpty());
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getLastSuccessfulBuild() {
    switch (type) {
      case DOCKER:
      case ECR:
        return;
      case ARTIFACTORY:
        switch (repositoryType) {
          case "any":
          case "docker":
            return;
          case "maven":
            Service service = Service.builder().appId(appId).artifactType(WAR).name("Some service").build();
            wingsPersistence.save(service);
            BuildDetails build =
                buildSourceService.getLastSuccessfulBuild(appId, artifactStream.getUuid(), settingAttribute.getUuid());
            assertNotNull(build);
            break;
          default:
            throw new IllegalArgumentException("invalid repo type");
        }
        break;

      default:
        Service service = Service.builder().appId(appId).artifactType(WAR).name("Some service").build();
        wingsPersistence.save(service);
        BuildDetails build =
            buildSourceService.getLastSuccessfulBuild(appId, artifactStream.getUuid(), settingAttribute.getUuid());
        assertNotNull(build);
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getGroupIds() {
    Set<String> groupIds;
    switch (type) {
      case JENKINS:
      case BAMBOO:
      case DOCKER:
      case ECR:
        try {
          groupIds = buildSourceService.getGroupIds(appId, jobName, settingAttribute.getUuid());
          fail("should throw excpetion");
        } catch (WingsException e) {
          // expected
          return;
        }
        break;
      case ARTIFACTORY:
        switch (repositoryType) {
          case "any":
            return;
          case "maven":
          case "docker":
            groupIds = buildSourceService.getGroupIds(appId, jobName, settingAttribute.getUuid());
            break;
          default:
            throw new IllegalArgumentException("invalid repo type: " + repositoryType);
        }
        break;
      case NEXUS:
        groupIds = buildSourceService.getGroupIds(appId, jobName, settingAttribute.getUuid());
        break;
      default:
        throw new IllegalArgumentException("invalid type: " + type);
    }
    assertFalse(groupIds.isEmpty());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void validateArtifactSource() {
    switch (type) {
      case JENKINS:
      case BAMBOO:
      case NEXUS:
        assertTrue(buildSourceService.validateArtifactSource(appId, settingAttribute.getUuid(), null));
        break;
      case ARTIFACTORY:
        if (repositoryType.equals("docker")) {
          return;
        }
        break;
      case DOCKER:
        assertTrue(buildSourceService.validateArtifactSource(
            appId, settingAttribute.getUuid(), artifactStream.getArtifactStreamAttributes()));
        break;
      case ECR:
        return;
      default:
        throw new IllegalArgumentException("invalid type: " + type);
    }
  }
}
