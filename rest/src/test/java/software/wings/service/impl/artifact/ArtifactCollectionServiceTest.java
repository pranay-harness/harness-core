package software.wings.service.impl.artifact;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Service;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactCollectionServiceTest extends WingsBaseTest {
  @Inject @Spy private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private ArtifactCollectionService artifactCollectionService;

  @Mock ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private BuildSourceService buildSourceService;
  @Mock private ServiceResourceService serviceResourceService;

  private Artifact.Builder artifactBuilder = anArtifact()
                                                 .withAppId(APP_ID)
                                                 .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                                 .withRevision("1.0")
                                                 .withDisplayName("DISPLAY_NAME")
                                                 .withCreatedAt(System.currentTimeMillis())
                                                 .withCreatedBy(EmbeddedUser.builder().uuid("USER_ID").build())
                                                 .withServiceIds(asList(SERVICE_ID));

  private Artifact artifact = artifactBuilder.build();

  @Before
  public void setUp() {
    when(wingsPersistence.get(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
  }

  @Test
  public void shouldCollectJenkinsArtifact() {
    BuildDetails jenkinsBuildDetails = getJenkinsBuildDetails();
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(jenkinsArtifactStream, jenkinsBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);

    Artifact collectedArtifact =
        artifactCollectionService.collectArtifact(APP_ID, ARTIFACT_STREAM_ID, jenkinsBuildDetails);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("3594");
    assertThat(collectedArtifact.getUrl()).isEqualTo("https://jenkins.harness.io/job/portal/3594/");
  }

  @Test
  public void shouldCollectS3Artifact() {
    BuildDetails s3BuildDetails = getS3BuildDetails();

    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .uuid(ARTIFACT_STREAM_ID)
                                                        .appId(APP_ID)
                                                        .sourceName("ARTIFACT_SOURCE")
                                                        .serviceId(SERVICE_ID)
                                                        .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(amazonS3ArtifactStream);
    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(amazonS3ArtifactStream, s3BuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);

    Artifact collectedArtifact = artifactCollectionService.collectArtifact(APP_ID, ARTIFACT_STREAM_ID, s3BuildDetails);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("appstack/apache-tomcat-8.5.15.tar.gz");
    assertThat(collectedArtifact.getUrl())
        .isEqualTo("https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-8.5.15.tar.gz");
  }

  private BuildDetails getS3BuildDetails() {
    Map<String, String> map = new HashMap<>();
    map.put(Constants.URL, "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-8.5.15.tar.gz");
    map.put(Constants.BUILD_NO, "appstack/apache-tomcat-8.5.15.tar.gz");
    map.put(Constants.BUCKET_NAME, "harness-catalogs");
    map.put(Constants.ARTIFACT_PATH, "appstack/apache-tomcat-8.5.15.tar.gz");
    map.put(Constants.KEY, "appstack/apache-tomcat-8.5.15.tar.gz");

    return aBuildDetails().withNumber("appstack/apache-tomcat-8.5.15.tar.gz").withBuildParameters(map).build();
  }

  @Test
  public void shouldCollectNewArtifactsDocker() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("latest").build();
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .settingId(SETTING_ID)
                                                    .build();

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(dockerArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(dockerArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("latest");
  }

  @Test
  public void shouldCollectNewArtifactsEcr() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("latest").build();
    EcrArtifactStream ecrArtifactStream = EcrArtifactStream.builder()
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .appId(APP_ID)
                                              .sourceName("ARTIFACT_SOURCE")
                                              .serviceId(SERVICE_ID)
                                              .settingId(SETTING_ID)
                                              .build();

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(ecrArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(ecrArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("latest");
  }

  @Test
  public void shouldCollectNewArtifactsGcr() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("latest").build();
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .appId(APP_ID)
                                              .sourceName("ARTIFACT_SOURCE")
                                              .serviceId(SERVICE_ID)
                                              .settingId(SETTING_ID)
                                              .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(gcrArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(gcrArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("latest");
  }

  @Test
  public void shouldCollectNewArtifactsAcr() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("latest").build();
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .appId(APP_ID)
                                              .sourceName("ARTIFACT_SOURCE")
                                              .serviceId(SERVICE_ID)
                                              .settingId(SETTING_ID)
                                              .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(acrArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(acrArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("latest");
  }

  @Test
  public void shouldCollectNewArtifactsAmi() {
    BuildDetails amiBuildDetails = aBuildDetails().withNumber("AMI-Image").withRevision("ImageId").build();
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .settingId(SETTING_ID)
                                                    .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(dockerArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(dockerArtifactStream, amiBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(amiBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("AMI-Image");
  }

  @Test
  public void shouldCollectNewArtifactsNexusDocker() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("latest").build();
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .uuid(ARTIFACT_STREAM_ID)
                                                  .appId(APP_ID)
                                                  .sourceName("ARTIFACT_SOURCE")
                                                  .serviceId(SERVICE_ID)
                                                  .settingId(SETTING_ID)
                                                  .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(nexusArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("latest");
  }

  @Test
  public void shouldCollectNewArtifactsNexus() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("1.1").withRevision("1.1").build();
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .uuid(ARTIFACT_STREAM_ID)
                                                  .appId(APP_ID)
                                                  .sourceName("ARTIFACT_SOURCE")
                                                  .serviceId(SERVICE_ID)
                                                  .settingId(SETTING_ID)
                                                  .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(nexusArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("1.1");
    assertThat(collectedArtifact.getRevision()).isEqualTo("1.1");
  }

  @Test
  public void shouldCollectNewArtifactsArtifactoryDocker() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber("latest").build();
    ArtifactoryArtifactStream artifactoryArtifactStream = getArtifactoryArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactoryArtifactStream);

    when(serviceResourceService.get(APP_ID, artifactoryArtifactStream.getServiceId(), false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(DOCKER).build());

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(artifactoryArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("latest");
  }

  @Test
  public void shouldCollectNewArtifactsArtifactoryGeneric() {
    BuildDetails artifactoryBuilds =
        aBuildDetails().withNumber("todolist.rpm").withArtifactPath("harness-rpm/todolist.rpm").build();
    ArtifactoryArtifactStream artifactoryArtifactStream = getArtifactoryArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactoryArtifactStream);

    when(serviceResourceService.get(APP_ID, artifactoryArtifactStream.getServiceId(), false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(RPM).build());

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(artifactoryArtifactStream, artifactoryBuilds);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID, 25))
        .thenReturn(asList(artifactoryBuilds));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("todolist.rpm");
    assertThat(collectedArtifact.getArtifactPath()).isEqualTo("harness-rpm/todolist.rpm");
  }

  @Test
  public void shouldCollectNewArtifactsArtifactoryMaven() {
    BuildDetails artifactoryBuilds = aBuildDetails().withNumber("1.1").build();
    ArtifactoryArtifactStream artifactoryArtifactStream = getArtifactoryArtifactStream();
    artifactoryArtifactStream.setRepositoryType("maven");
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactoryArtifactStream);

    when(serviceResourceService.get(APP_ID, artifactoryArtifactStream.getServiceId(), false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(WAR).build());

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(artifactoryArtifactStream, artifactoryBuilds);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getLastSuccessfulBuild(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID))
        .thenReturn(artifactoryBuilds);

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("1.1");
  }

  @Test
  public void shouldCollectNewArtifactsS3() {
    BuildDetails s3BuildDetails = getS3BuildDetails();
    AmazonS3ArtifactStream s3ArtifactStream = getS3ArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(s3ArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(s3ArtifactStream, s3BuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(s3BuildDetails));

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("appstack/apache-tomcat-8.5.15.tar.gz");
  }

  @Test
  public void shouldCollectNewArtifactsJenkins() {
    BuildDetails jenkinsBuildDetails = getJenkinsBuildDetails();
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(jenkinsArtifactStream, jenkinsBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(jenkinsBuildDetails));

    when(buildSourceService.getLastSuccessfulBuild(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID))
        .thenReturn(jenkinsBuildDetails);

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("3594");
  }

  @Test
  public void shouldCollectNewArtifactsBamboo() {
    BuildDetails bambooBuildDetails = aBuildDetails().withNumber("20").build();
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .settingId(SETTING_ID)
                                                    .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(bambooArtifactStream);

    Artifact newArtifact = ArtifactCollectionUtil.getArtifact(bambooArtifactStream, bambooBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getLastSuccessfulBuild(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID))
        .thenReturn(bambooBuildDetails);

    List<Artifact> collectedArtifacts = artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(collectedArtifacts).isNotNull().size().isEqualTo(1);
    Artifact collectedArtifact = collectedArtifacts.get(0);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("20");
  }

  private AmazonS3ArtifactStream getS3ArtifactStream() {
    return AmazonS3ArtifactStream.builder()
        .uuid(ARTIFACT_STREAM_ID)
        .appId(APP_ID)
        .sourceName("ARTIFACT_SOURCE")
        .serviceId(SERVICE_ID)
        .settingId(SETTING_ID)
        .build();
  }

  private JenkinsArtifactStream getJenkinsArtifactStream() {
    return JenkinsArtifactStream.builder()
        .uuid(ARTIFACT_STREAM_ID)
        .appId(APP_ID)
        .sourceName("ARTIFACT_SOURCE")
        .serviceId(SERVICE_ID)
        .settingId(SETTING_ID)
        .build();
  }

  private ArtifactoryArtifactStream getArtifactoryArtifactStream() {
    return ArtifactoryArtifactStream.builder()
        .uuid(ARTIFACT_STREAM_ID)
        .appId(APP_ID)
        .sourceName("ARTIFACT_SOURCE")
        .serviceId(SERVICE_ID)
        .settingId(SETTING_ID)
        .build();
  }

  private BuildDetails getJenkinsBuildDetails() {
    return aBuildDetails()
        .withNumber("3594")
        .withRevision("12345")
        .withBuildUrl("https://jenkins.harness.io/job/portal/3594/")
        .build();
  }
}
