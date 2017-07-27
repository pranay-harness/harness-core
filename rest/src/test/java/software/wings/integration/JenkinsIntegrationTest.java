package software.wings.integration;

import static org.junit.Assert.assertTrue;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfig.Builder;
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
    loginAdminUser();
    wingsPersistence.delete(
        wingsPersistence.createQuery(SettingAttribute.class).field("name").equal("Harness Jenkins"));
    SettingAttribute jenkinsSettingAttribute =
        aSettingAttribute()
            .withName("Harness Jenkins")
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(aJenkinsConfig()
                           .withAccountId(accountId)
                           .withJenkinsUrl("http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/")
                           .withUsername("admin")
                           .withPassword("admin".toCharArray())
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, jenkinsSettingAttribute);

    JenkinsConfig jenkinsConfig =
        (JenkinsConfig) wingsPersistence
            .executeGetOneQuery(
                wingsPersistence.createQuery(SettingAttribute.class).field("name").equal("Harness Jenkins"))
            .getValue();
    Assert.assertEquals("http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/", jenkinsConfig.getJenkinsUrl());
  }

  @Test
  public void testGetJobs() throws Exception {
    JenkinsConfig jenkinsConfig = Builder.aJenkinsConfig()
                                      .withJenkinsUrl(JENKINS_URL)
                                      .withPassword(JENKINS_PASSWORD)
                                      .withUsername(JENKINS_USERNAME)
                                      .build();
    List<JobDetails> jobs = jenkinsBuildService.getJobs(jenkinsConfig, Optional.empty());
    assertTrue(jobs.size() > 0);
  }
}
