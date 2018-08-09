package software.wings.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_JENKINS_CONNECTOR;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.Randomizer;
import software.wings.generator.Randomizer.Seed;
import software.wings.generator.SecretGenerator;
import software.wings.generator.SecretGenerator.SecretName;
import software.wings.generator.SettingGenerator;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.JenkinsBuildService;

import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 6/26/17.
 */
public class JenkinsIntegrationTest extends BaseIntegrationTest {
  @Inject private JenkinsBuildService jenkinsBuildService;
  @Inject SettingGenerator settingGenerator;
  @Inject SecretGenerator secretGenerator;

  private JenkinsConfig obtainJenkinsConfig() {
    final Seed seed = Randomizer.seed();
    SettingAttribute jenkinsSettingAttribute =
        settingGenerator.ensurePredefined(seed, new Owners(), HARNESS_JENKINS_CONNECTOR);

    return (JenkinsConfig) wingsPersistence.createQuery(SettingAttribute.class)
        .filter("name", "Harness Jenkins")
        .get()
        .getValue();
  }

  @Test
  public void testSettingsOverwrite() throws Exception {
    super.setUp();
    loginAdminUser();
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins"));

    JenkinsConfig jenkinsConfig = obtainJenkinsConfig();
    assertEquals("https://jenkins.wings.software", jenkinsConfig.getJenkinsUrl());
  }

  @Test
  @Ignore
  public void testGetJobs() {
    JenkinsConfig jenkinsConfig = obtainJenkinsConfig();

    jenkinsConfig.setPassword(secretGenerator.decryptToCharArray(new SecretName("harness_jenkins")));

    List<JobDetails> jobs = jenkinsBuildService.getJobs(jenkinsConfig, null, Optional.empty());
    assertFalse(jobs.isEmpty());
  }
}
