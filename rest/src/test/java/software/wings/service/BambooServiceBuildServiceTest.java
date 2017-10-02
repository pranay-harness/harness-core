package software.wings.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

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
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by anubhaw on 11/28/16.
 */
@Ignore
public class BambooServiceBuildServiceTest extends WingsBaseTest {
  @Inject BambooService bambooService;
  @Inject SettingsService settingsService;

  private BambooConfig bambooConfig =
      Builder.aBambooConfig()
          .withBambooUrl("http://ec2-54-144-126-230.compute-1.amazonaws.com:8085/rest/api/latest/")
          .withUsername("wingsbuild")
          .withPassword("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray())
          .build();

  @Test
  public void shouldFetchBambooSettings() {
    PageResponse<SettingAttribute> settingAttributes = settingsService.list(new PageRequest<>());
    SettingAttribute settingAttribute = settingsService.get("YcsuxTFqR6uH093foR_K5w-bamboo");
    System.out.println(settingAttribute.toString());
  }

  @Test
  public void shouldGetJobs() {
    System.out.println(bambooService.getPlanKeys(bambooConfig));
  }

  @Test
  public void shouldGetArtifactPaths() {}

  @Test
  public void shouldGetLastSuccessfulBuild() {
    System.out.println(bambooService.getLastSuccessfulBuild(bambooConfig, "TOD-TOD-JOB1"));
  }

  @Test
  public void shouldGetBuilds() {
    System.out.println(bambooService.getBuilds(bambooConfig, "TOD-TOD-JOB1", 50));
  }

  @Test
  public void shouldGetBuildArtifacts() throws IOException, URISyntaxException {
    ListNotifyResponseData listNotifyResponseData = bambooService.downloadArtifacts(
        bambooConfig, "TOD-TOD-JOB1", "11", Lists.newArrayList("*"), "DELEGATE_ID", "TASK_ID", "ACCOUNT_ID");
    List data = listNotifyResponseData.getData();
    assert data.size() > 0;
  }
}
