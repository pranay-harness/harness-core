/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.verification;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRIRAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.SplunkConfig;
import software.wings.integration.IntegrationTestBase;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.splunk.Service;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SplunkIntegrationTest extends IntegrationTestBase {
  private static final String SPLUNK_CLOUD_URL = "https://api-prd-p-429h4vj2lsng.cloud.splunk.com:8089";
  @Inject SplunkDelegateService splunkDelegateService; // = new SplunkDelegateServiceImpl();
  @Inject private ScmSecret scmSecret;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void initSplunkServiceWithToken()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    SplunkConfig config =
        SplunkConfig.builder()
            .accountId(accountId)
            .splunkUrl(SPLUNK_CLOUD_URL)
            .username(scmSecret.decryptToString(new SecretName("splunk_cloud_username")))
            .password(scmSecret.decryptToString(new SecretName("splunk_cloud_password")).toCharArray())
            .build();

    Method method =
        splunkDelegateService.getClass().getDeclaredMethod("initSplunkServiceWithToken", SplunkConfig.class);
    method.setAccessible(true);
    Object r = method.invoke(splunkDelegateService, config);
    assertThat(((Service) r).getToken().startsWith("Splunk")).isTrue();
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void initSplunkServiceWithBasicAuth()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    SplunkConfig config =
        SplunkConfig.builder()
            .accountId(accountId)
            .splunkUrl(SPLUNK_CLOUD_URL)
            .username(scmSecret.decryptToString(new SecretName("splunk_cloud_username")))
            .password(scmSecret.decryptToString(new SecretName("splunk_cloud_password")).toCharArray())
            .build();

    Method method =
        splunkDelegateService.getClass().getDeclaredMethod("initSplunkServiceWithBasicAuth", SplunkConfig.class);
    method.setAccessible(true);
    Object r = method.invoke(splunkDelegateService, config);
    assertThat(((Service) r).getToken().startsWith("Basic")).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void splunkLogQueryCustomHostFieldTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String expectedQuery =
        "search testQuery myHostNameField = harness.test.host.name | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t| table _time, _raw,cluster_label, myHostNameField | stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,myHostNameField";
    Method method = splunkDelegateService.getClass().getDeclaredMethod(
        "getQuery", String.class, String.class, String.class, boolean.class);
    method.setAccessible(true);

    Object r = method.invoke(splunkDelegateService, "testQuery", "myHostNameField", "harness.test.host.name", false);
    String formedQuery = (String) r;
    assertThat(formedQuery).isEqualTo(expectedQuery);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void splunkLogQuerAdvancedQueryTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String advancedQuery = "my advanced test query";
    String expectedQuery = advancedQuery
        + " myHostNameField = harness.test.host.name | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t| table _time, _raw,cluster_label, myHostNameField | stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,myHostNameField";
    Method method = splunkDelegateService.getClass().getDeclaredMethod(
        "getQuery", String.class, String.class, String.class, boolean.class);
    method.setAccessible(true);

    Object r = method.invoke(splunkDelegateService, advancedQuery, "myHostNameField", "harness.test.host.name", true);
    String formedQuery = (String) r;
    assertThat(formedQuery).isEqualTo(expectedQuery);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void splunkInvalidUrl() {
    SplunkConfig config =
        SplunkConfig.builder()
            .accountId(accountId)
            .splunkUrl("https://www.google.com/")
            .username(scmSecret.decryptToString(new SecretName("splunk_cloud_username")))
            .password(scmSecret.decryptToString(new SecretName("splunk_cloud_password")).toCharArray())
            .build();

    try {
      splunkDelegateService.validateConfig(config, Lists.newArrayList());
      Assert.fail("Validated invalid url");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e.getMessage())
          .isEqualTo("Can not reach url " + config.getSplunkUrl() + " to create splunk serach job");
    }
  }
}
