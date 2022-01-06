/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

/**
 * Created by rsingh on 6/26/17.
 */
public class JenkinsIntegrationTestBase extends IntegrationTestBase {
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
  //  @Owner(emails = UNKNOWN)
  //  public void testSettingsOverwrite() throws Exception {
  //    super.setUp();
  //    loginAdminUser();
  //    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins"));
  //
  //    JenkinsConfig jenkinsConfig = obtainJenkinsConfig();
  //    assertThat( jenkinsConfig.getJenkinsUrl()).isEqualTo("https://jenkins.wings.software");
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Ignore("TODO: please provide clear motivation why this test is ignored")
  //  public void testGetJobs() {
  //    JenkinsConfig jenkinsConfig = obtainJenkinsConfig();
  //
  //    jenkinsConfig.setPassword(scmSecret.decryptToCharArray(new SecretName("harness_jenkins")));
  //
  //    List<JobDetails> jobs = jenkinsBuildService.getJobs(jenkinsConfig, null, Optional.empty());
  //    assertThat(jobs.isEmpty()).isFalse();
  //  }
}
