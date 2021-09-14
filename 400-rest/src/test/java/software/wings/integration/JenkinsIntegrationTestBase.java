/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
