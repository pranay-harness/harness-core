package software.wings.service;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.service.intfc.SettingsService;

import java.io.InputStream;

@Ignore
@Slf4j
public class BambooServiceBuildServiceTest extends WingsBaseTest {
  @Inject BambooService bambooService;
  @Inject SettingsService settingsService;
  @Inject private ScmSecret scmSecret;
  private BambooConfig bambooConfig;

  @Before
  public void setUp() {
    bambooConfig = BambooConfig.builder()
                       .bambooUrl("http://ec2-54-144-126-230.compute-1.amazonaws.com:8085/rest/api/latest/")
                       .username("wingsbuild")
                       .password(scmSecret.decryptToCharArray(new SecretName("bamboo_config_password")))
                       .build();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFetchBambooSettings() {
    PageResponse<SettingAttribute> settingAttributes = settingsService.list(new PageRequest<>(), null, null);
    SettingAttribute settingAttribute = settingsService.get("YcsuxTFqR6uH093foR_K5w-bamboo");
    logger.info(settingAttribute.toString());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetJobs() {
    logger.info(bambooService.getPlanKeys(bambooConfig, null).toString());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactPaths() {}

  @Test
  @Category(UnitTests.class)
  public void shouldGetLastSuccessfulBuild() {
    logger.info(bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TOD-JOB1").toString());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetBuilds() {
    logger.info(bambooService.getBuilds(bambooConfig, null, "TOD-TOD-JOB1", 50).toString());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetBuildArtifacts() {
    Pair<String, InputStream> stringInputStreamPair =
        bambooService.downloadArtifact(bambooConfig, null, "TOD-TOD-JOB1", "11", "*");
    logger.info(stringInputStreamPair.getKey());
  }
}
