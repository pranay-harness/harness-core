/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.ANUBHAW;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldFetchBambooSettings() {
    PageResponse<SettingAttribute> settingAttributes = settingsService.list(new PageRequest<>(), null, null);
    SettingAttribute settingAttribute = settingsService.get("YcsuxTFqR6uH093foR_K5w-bamboo");
    log.info(settingAttribute.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetJobs() {
    log.info(bambooService.getPlanKeys(bambooConfig, null).toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetArtifactPaths() {}

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetLastSuccessfulBuild() {
    log.info(bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TOD-JOB1").toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetBuilds() {
    log.info(bambooService.getBuilds(bambooConfig, null, "TOD-TOD-JOB1", Arrays.asList(), 50).toString());
  }
}
