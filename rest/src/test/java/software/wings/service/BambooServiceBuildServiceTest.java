package software.wings.service;

import com.google.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.BambooConfig.Builder;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.service.intfc.SettingsService;

import java.io.InputStream;

/**
 * Created by anubhaw on 11/28/16.
 */
@Ignore
public class BambooServiceBuildServiceTest extends WingsBaseTest {
  @Inject BambooService bambooService;
  @Inject SettingsService settingsService;

  private BambooConfig bambooConfig = Builder.aBambooConfig()
                                          .withBamboosUrl("http://localhost:8085/rest/api/latest/")
                                          .withUsername("admin")
                                          .withPassword("admin")
                                          .build();

  @Test
  public void shouldFetchBambooSettings() {
    PageResponse<SettingAttribute> settingAttributes = settingsService.list(new PageRequest<>());
    SettingAttribute settingAttribute = settingsService.get("YcsuxTFqR6uH093foR_K5w-bamboo");
    System.out.println(settingAttribute.toString());
  }

  @Test
  public void shouldGetJobs() {
    System.out.println(bambooService.getJobKeys(bambooConfig));
  }

  @Test
  public void shouldGetArtifactPaths() {}

  @Test
  public void shouldGetLastSuccessfulBuild() {
    System.out.println(bambooService.getLastSuccessfulBuild(bambooConfig, "TOD-TOD-JOB1"));
  }

  @Test
  public void shouldGetBuilds() {
    System.out.println(bambooService.getBuildsForJob(bambooConfig, "TOD-TOD-JOB1", 50));
  }

  @Test
  public void shouldGetBuildArtifacts() {
    Pair<String, InputStream> stringInputStreamPair =
        bambooService.downloadArtifact(bambooConfig, "TOD-TOD-JOB1", "11", "*");
    System.out.println(stringInputStreamPair.getKey());
  }
}
