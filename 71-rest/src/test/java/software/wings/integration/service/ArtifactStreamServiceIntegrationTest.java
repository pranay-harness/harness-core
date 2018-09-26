package software.wings.integration.service;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Service.builder;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_ARTIFACTORY_CONNECTOR;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.rule.AuthorRule.Author;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.generator.AccountGenerator;
import software.wings.generator.AccountGenerator.Accounts;
import software.wings.generator.ApplicationGenerator;
import software.wings.generator.ApplicationGenerator.Applications;
import software.wings.generator.ArtifactStreamGenerator;
import software.wings.generator.OwnerManager;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.Randomizer.Seed;
import software.wings.generator.ServiceGenerator;
import software.wings.generator.SettingGenerator;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by sgurubelli on 7/28/17.
 */
@SetupScheduler
public class ArtifactStreamServiceIntegrationTest extends BaseIntegrationTest {
  @Inject ServiceResourceService serviceResourceService;
  @Inject private AccountGenerator accountGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private ArtifactoryService artifactoryService;

  private static final String APP_NAME = "APP_NAME_ARTIFACT";
  private static final String SERVICE_NAME = "SERVICE_NAME_SERVICE";

  private Application application;
  private SettingAttribute settingAttribute;
  private ArtifactStream artifactStream;
  private Service service;
  final Seed seed = new Seed(0);

  private JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                            .appId(APP_ID)
                                                            .uuid(ARTIFACT_STREAM_ID)
                                                            .sourceName("SOURCE_NAME")
                                                            .settingId(SETTING_ID)
                                                            .jobname("JOB")
                                                            .serviceId(SERVICE_ID)
                                                            .artifactPaths(asList("*WAR"))
                                                            .build();

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    final Seed seed = new Seed(0);
    Owners owners = new Owners();
    Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    owners.add(account);
    settingAttribute = settingGenerator.ensurePredefined(seed, owners, HARNESS_ARTIFACTORY_CONNECTOR);
    application =
        owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    service = serviceGenerator.ensureService(
        seed, owners, builder().name("Artifactory Service").artifactType(ArtifactType.WAR).build());
  }

  @Test
  @Author(emails = "aaditi.joag@harness.io")
  public void shouldTestCRUDArtifactoryArtifactStream() {
    artifactStream = artifactStreamGenerator.ensureArtifactStream(seed,
        ArtifactoryArtifactStream.builder()
            .appId(application.getAppId())
            .serviceId(service.getUuid())
            .name("harness-maven_todolist-war")
            .sourceName(settingAttribute.getName())
            .jobname("harness-maven")
            .artifactPattern("io/harness/todolist/todolist/*/*.war")
            .settingId(settingAttribute.getUuid())
            .metadataOnly(false)
            .build());
    artifactStream.setServiceId(service.getUuid());
    artifactStream.setAppId(application.getAppId());

    ArtifactoryArtifactStream created =
        (ArtifactoryArtifactStream) artifactStreamService.get(application.getAppId(), artifactStream.getUuid());
    assertEquals(artifactStream.getUuid(), created.getUuid());
    assertEquals(artifactStream.isMetadataOnly(), created.isMetadataOnly());
    assertEquals("harness-maven", created.getJobname());

    created.setMetadataOnly(true);
    created.setJobname("harness-rpm");
    ArtifactoryArtifactStream updated = (ArtifactoryArtifactStream) artifactStreamService.forceUpdate(created);
    assertEquals(created.isMetadataOnly(), updated.isMetadataOnly());
    assertEquals("harness-rpm", updated.getJobname());

    artifactStreamService.delete(application.getUuid(), created.getUuid());
    ArtifactoryArtifactStream deleted =
        (ArtifactoryArtifactStream) artifactStreamService.get(application.getAppId(), artifactStream.getUuid());
    assertNull(deleted);
  }

  @Ignore
  @Test
  public void testAddArtifactStream() {
    loginAdminUser();
    Application app = createApp(APP_NAME);
    Service service = createService(app.getAppId(),
        ImmutableMap.of(
            "name", SERVICE_NAME, "description", randomText(40), "appId", app.getUuid(), "artifactType", WAR.name()));
    addArtifactStream(app.getAppId(), service.getUuid());
  }

  private void addArtifactStream(String appId, String serviceId) {
    WebTarget target = client.target(API_BASE + "/artifactstreams/?appId=" + appId);
    SettingAttribute settingAttribute =
        wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins").get();

    jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                .appId(APP_ID)
                                .uuid(ARTIFACT_STREAM_ID)
                                .sourceName("todolistwar")
                                .settingId(settingAttribute.getUuid())
                                .jobname("toddolistwar")
                                .serviceId(serviceId)
                                .artifactPaths(asList("target/todolistwar"))
                                .build();
    RestResponse<ArtifactStream> response = getRequestBuilder(target).post(
        entity(jenkinsArtifactStream, APPLICATION_JSON), new GenericType<RestResponse<ArtifactStream>>() {

        });
    assertThat(response).isNotNull();
  }
}
