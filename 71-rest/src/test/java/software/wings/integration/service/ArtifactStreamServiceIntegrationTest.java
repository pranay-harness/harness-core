package software.wings.integration.service;

import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.SetupScheduler;

/**
 * Created by sgurubelli on 7/28/17.
 */
@SetupScheduler
public class ArtifactStreamServiceIntegrationTest extends BaseIntegrationTest {
  //  @Inject ServiceResourceService serviceResourceService;
  //  @Inject private AccountGenerator accountGenerator;
  //  @Inject private SettingGenerator settingGenerator;
  //  @Inject private OwnerManager ownerManager;
  //  @Inject private ApplicationGenerator applicationGenerator;
  //  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  //  @Inject private ServiceGenerator serviceGenerator;
  //  @Inject private ArtifactService artifactService;
  //  @Inject private ArtifactStreamService artifactStreamService;
  //  @Inject private TimeLimiter timeLimiter;
  //  @Inject private ArtifactoryService artifactoryService;
  //
  //  private static final String APP_NAME = "APP_NAME_ARTIFACT";
  //  private static final String SERVICE_NAME = "SERVICE_NAME_SERVICE";
  //
  //  private Application application;
  //  private SettingAttribute settingAttribute;
  //  private ArtifactStream artifactStream;
  //  private Service service;
  //  final Seed seed = new Seed(0);
  //
  //  private JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
  //                                                            .appId(APP_ID)
  //                                                            .uuid(ARTIFACT_STREAM_ID)
  //                                                            .sourceName("SOURCE_NAME")
  //                                                            .settingId(SETTING_ID)
  //                                                            .jobname("JOB")
  //                                                            .serviceId(SERVICE_ID)
  //                                                            .artifactPaths(asList("*WAR"))
  //                                                            .build();
  //
  //  @Before
  //  public void setUp() throws Exception {
  //    initMocks(this);
  //    final Seed seed = new Seed(0);
  //    Owners owners = new Owners();
  //    Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
  //    owners.add(account);
  //    settingAttribute = settingGenerator.ensurePredefined(seed, owners, HARNESS_ARTIFACTORY_CONNECTOR);
  //    application =
  //        owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners,
  //        Applications.GENERIC_TEST));
  //    service = serviceGenerator.ensureService(
  //        seed, owners, builder().name("Artifactory Service").artifactType(ArtifactType.WAR).build());
  //  }
  //
  //  @Test
  //  @Owner(emails = "aaditi.joag@harness.io")
  //  public void shouldTestCRUDArtifactoryArtifactStream() {
  //    artifactStream = artifactStreamGenerator.ensureArtifactStream(seed,
  //        ArtifactoryArtifactStream.builder()
  //            .appId(application.getAppId())
  //            .serviceId(service.getUuid())
  //            .name("harness-maven_todolist-war")
  //            .sourceName(settingAttribute.getName())
  //            .jobname("harness-maven")
  //            .artifactPattern("io/harness/todolist/todolist/*/*.war")
  //            .settingId(settingAttribute.getUuid())
  //            .metadataOnly(false)
  //            .build());
  //    artifactStream.setServiceId(service.getUuid());
  //    artifactStream.setAppId(application.getAppId());
  //
  //    ArtifactoryArtifactStream created =
  //        (ArtifactoryArtifactStream) artifactStreamService.get(application.getAppId(), artifactStream.getUuid());
  //    assertEquals(artifactStream.getUuid(), created.getUuid());
  //    assertEquals(artifactStream.isMetadataOnly(), created.isMetadataOnly());
  //    assertEquals("harness-maven", created.getJobname());
  //
  //    created.setMetadataOnly(true);
  //    created.setJobname("harness-rpm");
  //    ArtifactoryArtifactStream updated = (ArtifactoryArtifactStream) artifactStreamService.forceUpdate(created);
  //    assertEquals(created.isMetadataOnly(), updated.isMetadataOnly());
  //    assertEquals("harness-rpm", updated.getJobname());
  //
  //    artifactStreamService.delete(application.getUuid(), created.getUuid());
  //    ArtifactoryArtifactStream deleted =
  //        (ArtifactoryArtifactStream) artifactStreamService.get(application.getAppId(), artifactStream.getUuid());
  //    assertNull(deleted);
  //  }
  //
  //  @Ignore
  //  @Test
  //  public void testAddArtifactStream() {
  //    loginAdminUser();
  //    Application app = createApp(APP_NAME);
  //    Service service = createService(app.getAppId(),
  //        ImmutableMap.of(
  //            "name", SERVICE_NAME, "description", randomText(40), "appId", app.getUuid(), "artifactType",
  //            WAR.name()));
  //    addArtifactStream(app.getAppId(), service.getUuid());
  //  }
  //
  //  private void addArtifactStream(String appId, String serviceId) {
  //    WebTarget target = client.target(API_BASE + "/artifactstreams/?appId=" + appId);
  //    SettingAttribute settingAttribute =
  //        wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins").get();
  //
  //    jenkinsArtifactStream = JenkinsArtifactStream.builder()
  //                                .appId(APP_ID)
  //                                .uuid(ARTIFACT_STREAM_ID)
  //                                .sourceName("todolistwar")
  //                                .settingId(settingAttribute.getUuid())
  //                                .jobname("toddolistwar")
  //                                .serviceId(serviceId)
  //                                .artifactPaths(asList("target/todolistwar"))
  //                                .build();
  //    RestResponse<ArtifactStream> response = getRequestBuilder(target).post(
  //        entity(jenkinsArtifactStream, APPLICATION_JSON), new GenericType<RestResponse<ArtifactStream>>() {
  //
  //        });
  //    assertThat(response).isNotNull();
  //  }
}
