package software.wings.service.impl.artifact;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
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
import java.util.Map;

public class ArtifactCollectionServiceTest extends WingsBaseTest {
  public static final String LATEST_BUILD_NUMBER = "latest";
  @Inject @Spy private WingsPersistence wingsPersistence;
  @InjectMocks @Inject @Named("ArtifactCollectionService") private ArtifactCollectionService artifactCollectionService;
  @Inject private ArtifactCollectionUtil artifactCollectionUtil;

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
    when(wingsPersistence.getWithAppId(Artifact.class, APP_ID, ARTIFACT_ID)).thenReturn(artifact);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectJenkinsArtifact() {
    BuildDetails jenkinsBuildDetails = getJenkinsBuildDetails();
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    Artifact newArtifact = artifactCollectionUtil.getArtifact(jenkinsArtifactStream, jenkinsBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);

    Artifact collectedArtifact =
        artifactCollectionService.collectArtifact(APP_ID, ARTIFACT_STREAM_ID, jenkinsBuildDetails);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo("3594");
    assertThat(collectedArtifact.getUrl()).isEqualTo("https://jenkins.harness.io/job/portal/3594/");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectS3Artifact() {
    BuildDetails s3BuildDetails = getS3BuildDetails();

    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .uuid(ARTIFACT_STREAM_ID)
                                                        .appId(APP_ID)
                                                        .sourceName("ARTIFACT_SOURCE")
                                                        .serviceId(SERVICE_ID)
                                                        .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(amazonS3ArtifactStream);
    Artifact newArtifact = artifactCollectionUtil.getArtifact(amazonS3ArtifactStream, s3BuildDetails);
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
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsDocker() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(LATEST_BUILD_NUMBER).build();
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .settingId(SETTING_ID)
                                                    .build();

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(dockerArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(dockerArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, dockerArtifactStream, LATEST_BUILD_NUMBER);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(LATEST_BUILD_NUMBER);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsEcr() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(LATEST_BUILD_NUMBER).build();
    EcrArtifactStream ecrArtifactStream = EcrArtifactStream.builder()
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .appId(APP_ID)
                                              .sourceName("ARTIFACT_SOURCE")
                                              .serviceId(SERVICE_ID)
                                              .settingId(SETTING_ID)
                                              .build();

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(ecrArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(ecrArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, ecrArtifactStream, LATEST_BUILD_NUMBER);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(LATEST_BUILD_NUMBER);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsGcr() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(LATEST_BUILD_NUMBER).build();
    GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .appId(APP_ID)
                                              .sourceName("ARTIFACT_SOURCE")
                                              .serviceId(SERVICE_ID)
                                              .settingId(SETTING_ID)
                                              .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(gcrArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(gcrArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, gcrArtifactStream, LATEST_BUILD_NUMBER);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(LATEST_BUILD_NUMBER);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsAcr() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(LATEST_BUILD_NUMBER).build();
    AcrArtifactStream acrArtifactStream = AcrArtifactStream.builder()
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .appId(APP_ID)
                                              .sourceName("ARTIFACT_SOURCE")
                                              .serviceId(SERVICE_ID)
                                              .settingId(SETTING_ID)
                                              .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(acrArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(acrArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, acrArtifactStream, LATEST_BUILD_NUMBER);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(LATEST_BUILD_NUMBER);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsAmi() {
    String buildNumber = "AMI-Image";
    BuildDetails amiBuildDetails = aBuildDetails().withNumber(buildNumber).withRevision("ImageId").build();
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .settingId(SETTING_ID)
                                                    .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(dockerArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(dockerArtifactStream, amiBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(amiBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, dockerArtifactStream, buildNumber);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(buildNumber);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsNexusDocker() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(LATEST_BUILD_NUMBER).build();
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .uuid(ARTIFACT_STREAM_ID)
                                                  .appId(APP_ID)
                                                  .sourceName("ARTIFACT_SOURCE")
                                                  .serviceId(SERVICE_ID)
                                                  .settingId(SETTING_ID)
                                                  .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(nexusArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, nexusArtifactStream, LATEST_BUILD_NUMBER);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(LATEST_BUILD_NUMBER);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsNexus() {
    String buildNUmber = "1.1";
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(buildNUmber).withRevision(buildNUmber).build();
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .uuid(ARTIFACT_STREAM_ID)
                                                  .appId(APP_ID)
                                                  .sourceName("ARTIFACT_SOURCE")
                                                  .serviceId(SERVICE_ID)
                                                  .settingId(SETTING_ID)
                                                  .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(nexusArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, nexusArtifactStream, buildNUmber);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(buildNUmber);
    assertThat(collectedArtifact.getRevision()).isEqualTo(buildNUmber);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsArtifactoryDocker() {
    BuildDetails dockerBuildDetails = aBuildDetails().withNumber(LATEST_BUILD_NUMBER).build();
    ArtifactoryArtifactStream artifactoryArtifactStream = getArtifactoryArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactoryArtifactStream);

    when(serviceResourceService.get(APP_ID, artifactoryArtifactStream.getServiceId(), false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(DOCKER).build());

    Artifact newArtifact = artifactCollectionUtil.getArtifact(artifactoryArtifactStream, dockerBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(dockerBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, artifactoryArtifactStream, LATEST_BUILD_NUMBER);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(LATEST_BUILD_NUMBER);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsArtifactoryGeneric() {
    String buildNumber = "todolist.rpm";
    BuildDetails artifactoryBuilds =
        aBuildDetails().withNumber(buildNumber).withArtifactPath("harness-rpm/todolist.rpm").build();
    ArtifactoryArtifactStream artifactoryArtifactStream = getArtifactoryArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactoryArtifactStream);

    when(serviceResourceService.get(APP_ID, artifactoryArtifactStream.getServiceId(), false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(RPM).build());

    Artifact newArtifact = artifactCollectionUtil.getArtifact(artifactoryArtifactStream, artifactoryBuilds);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(artifactoryBuilds));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, artifactoryArtifactStream, buildNumber);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(buildNumber);
    assertThat(collectedArtifact.getArtifactPath()).isEqualTo("harness-rpm/todolist.rpm");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsS3() {
    BuildDetails s3BuildDetails = getS3BuildDetails();
    String buildNumber = s3BuildDetails.getNumber();
    AmazonS3ArtifactStream s3ArtifactStream = getS3ArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(s3ArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(s3ArtifactStream, s3BuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(s3BuildDetails));

    Artifact collectedArtifact = artifactCollectionService.collectNewArtifacts(APP_ID, s3ArtifactStream, buildNumber);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(buildNumber);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsJenkins() {
    BuildDetails jenkinsBuildDetails = getJenkinsBuildDetails();
    String buildNUmber = jenkinsBuildDetails.getNumber();
    JenkinsArtifactStream jenkinsArtifactStream = getJenkinsArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(jenkinsArtifactStream, jenkinsBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(jenkinsBuildDetails));

    when(buildSourceService.getLastSuccessfulBuild(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID))
        .thenReturn(jenkinsBuildDetails);

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, jenkinsArtifactStream, buildNUmber);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(buildNUmber);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNewArtifactsBamboo() {
    String buildNumber = "20";
    BuildDetails bambooBuildDetails = aBuildDetails().withNumber(buildNumber).build();
    BambooArtifactStream bambooArtifactStream = BambooArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .appId(APP_ID)
                                                    .sourceName("ARTIFACT_SOURCE")
                                                    .serviceId(SERVICE_ID)
                                                    .settingId(SETTING_ID)
                                                    .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(bambooArtifactStream);

    Artifact newArtifact = artifactCollectionUtil.getArtifact(bambooArtifactStream, bambooBuildDetails);
    when(artifactService.create(any(Artifact.class))).thenReturn(newArtifact);
    when(buildSourceService.getBuilds(APP_ID, ARTIFACT_STREAM_ID, SETTING_ID)).thenReturn(asList(bambooBuildDetails));

    Artifact collectedArtifact =
        artifactCollectionService.collectNewArtifacts(APP_ID, bambooArtifactStream, buildNumber);
    assertThat(collectedArtifact).isNotNull();
    assertThat(collectedArtifact.getBuildNo()).isEqualTo(buildNumber);
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
