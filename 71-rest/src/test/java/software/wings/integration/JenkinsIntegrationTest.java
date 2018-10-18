package software.wings.integration;

/**
 * Created by rsingh on 6/26/17.
 */
public class JenkinsIntegrationTest extends BaseIntegrationTest {
  //  @Inject private JenkinsBuildService jenkinsBuildService;
  //  @Inject SettingGenerator settingGenerator;
  //  @Inject private ScmSecret scmSecret;
  //
  //  private JenkinsConfig obtainJenkinsConfig() {
  //    final Seed seed = Randomizer.seed();
  //    SettingAttribute jenkinsSettingAttribute =
  //        settingGenerator.ensurePredefined(seed, new Owners(), HARNESS_JENKINS_CONNECTOR);
  //
  //    return (JenkinsConfig) wingsPersistence.createQuery(SettingAttribute.class)
  //        .filter("name", "Harness Jenkins")
  //        .get()
  //        .getValue();
  //  }
  //
  //  @Test
  //  public void testSettingsOverwrite() throws Exception {
  //    super.setUp();
  //    loginAdminUser();
  //    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins"));
  //
  //    JenkinsConfig jenkinsConfig = obtainJenkinsConfig();
  //    assertEquals("https://jenkins.wings.software", jenkinsConfig.getJenkinsUrl());
  //  }
  //
  //  @Test
  //  @Ignore
  //  public void testGetJobs() {
  //    JenkinsConfig jenkinsConfig = obtainJenkinsConfig();
  //
  //    jenkinsConfig.setPassword(scmSecret.decryptToCharArray(new SecretName("harness_jenkins")));
  //
  //    List<JobDetails> jobs = jenkinsBuildService.getJobs(jenkinsConfig, null, Optional.empty());
  //    assertFalse(jobs.isEmpty());
  //  }
}
