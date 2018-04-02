package software.wings.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.JenkinsBuildService;

import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 6/26/17.
 */
public class JenkinsIntegrationTest extends BaseIntegrationTest {
  @Inject private JenkinsBuildService jenkinsBuildService;

  @Test
  public void testSettingsOverwrite() throws Exception {
    super.setUp();
    loginAdminUser();
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins"));
    SettingAttribute jenkinsSettingAttribute =
        aSettingAttribute()
            .withName("Harness Jenkins")
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(JenkinsConfig.builder()
                           .accountId(accountId)
                           .jenkinsUrl("http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/")
                           .username("admin")
                           .password("admin".toCharArray())
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, jenkinsSettingAttribute);

    JenkinsConfig jenkinsConfig =
        (JenkinsConfig) wingsPersistence
            .executeGetOneQuery(wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins"))
            .getValue();
    assertEquals("http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/", jenkinsConfig.getJenkinsUrl());
  }

  @Test
  public void testGetJobs() {
    JenkinsConfig jenkinsConfig =
        JenkinsConfig.builder().jenkinsUrl(JENKINS_URL).password(JENKINS_PASSWORD).username(JENKINS_USERNAME).build();
    List<JobDetails> jobs = jenkinsBuildService.getJobs(jenkinsConfig, null, Optional.empty());
    assertFalse(jobs.isEmpty());
  }
}
