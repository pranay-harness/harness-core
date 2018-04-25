package software.wings.service;

import com.google.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
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
  private static final Logger logger = LoggerFactory.getLogger(BambooServiceBuildServiceTest.class);

  @Inject BambooService bambooService;
  @Inject SettingsService settingsService;

  private BambooConfig bambooConfig =
      BambooConfig.builder()
          .bambooUrl("http://ec2-54-144-126-230.compute-1.amazonaws.com:8085/rest/api/latest/")
          .username("wingsbuild")
          .password("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray())
          .build();

  @Test
  public void shouldFetchBambooSettings() {
    PageResponse<SettingAttribute> settingAttributes = settingsService.list(new PageRequest<>());
    SettingAttribute settingAttribute = settingsService.get("YcsuxTFqR6uH093foR_K5w-bamboo");
    logger.info(settingAttribute.toString());
  }

  @Test
  public void shouldGetJobs() {
    logger.info(bambooService.getPlanKeys(bambooConfig, null).toString());
  }

  @Test
  public void shouldGetArtifactPaths() {}

  @Test
  public void shouldGetLastSuccessfulBuild() {
    logger.info(bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TOD-JOB1").toString());
  }

  @Test
  public void shouldGetBuilds() {
    logger.info(bambooService.getBuilds(bambooConfig, null, "TOD-TOD-JOB1", 50).toString());
  }

  @Test
  public void shouldGetBuildArtifacts() {
    Pair<String, InputStream> stringInputStreamPair =
        bambooService.downloadArtifact(bambooConfig, null, "TOD-TOD-JOB1", "11", "*");
    logger.info(stringInputStreamPair.getKey());
  }
}
