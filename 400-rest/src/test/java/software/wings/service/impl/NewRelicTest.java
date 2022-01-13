/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRIRAM;

import static software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl.METRIC_NAME_NON_SPECIAL_CHARS;
import static software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl.METRIC_NAME_SPECIAL_CHARS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.time.Timestamp;

import software.wings.WingsBaseTest;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by rsingh on 10/10/17.
 */
public class NewRelicTest extends WingsBaseTest {
  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private ScmSecret scmSecret;
  @Inject private NewRelicService newRelicService;
  private NewRelicConfig newRelicConfig;
  private String accountId;
  static final String NEW_RELIC_DATE_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    newRelicConfig = NewRelicConfig.builder()
                         .accountId(accountId)
                         .newRelicUrl("https://api.newrelic.com")
                         .apiKey(scmSecret.decryptToCharArray(new SecretName("new_relic_api_key")))
                         .build();
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void cvdemo() throws IOException {
    // DO NOT REMOVE CV_DEMO FEATURE FLAG
    FeatureName.valueOf("CV_DEMO");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void testTimeStampCreations() {
    final SimpleDateFormat dateFormatter = new SimpleDateFormat(NEW_RELIC_DATE_FORMAT);
    dateFormatter.format(new Date(Timestamp.minuteBoundary(1513463100000L))).equals("2017-12-16T14:25:00-0800");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testMetricsBatching() {
    Set<NewRelicMetric> metricNames =
        Sets.newHashSet(NewRelicMetric.builder().name("WebTransaction/456/load+test-some/with_under_score1").build(),
            NewRelicMetric.builder().name("WebTransaction/456/load+test-some/with_under_score2").build(),
            NewRelicMetric.builder().name("1292Name/456/load test-some/with space_and_underscore").build(),
            NewRelicMetric.builder().name("WebTransaction/special char %?name=s1&value=v1").build(),
            NewRelicMetric.builder().name("WebTransaction/special char %?name=s2&value=v2").build(),
            NewRelicMetric.builder()
                .name("WebTransaction/SpringController/${server.error.path:${error.path:/error (GET)")
                .build(),
            NewRelicMetric.builder()
                .name("WebTransaction/SpringController/$server.error.path:$error.path:/error}} (GET)")
                .build());

    Map<String, List<Set<String>>> batchMetricsToCollect =
        NewRelicDelgateServiceImpl.batchMetricsToCollect(metricNames, true);
    assertThat(batchMetricsToCollect).hasSize(2);
    List<Set<String>> nonSpecialCharBatches = batchMetricsToCollect.get(METRIC_NAME_NON_SPECIAL_CHARS);
    assertThat(nonSpecialCharBatches).hasSize(1);
    assertThat(nonSpecialCharBatches.get(0).contains("WebTransaction/456/load+test-some/with_under_score1")).isTrue();
    assertThat(nonSpecialCharBatches.get(0).contains("WebTransaction/456/load+test-some/with_under_score2")).isTrue();
    assertThat(nonSpecialCharBatches.get(0).contains("1292Name/456/load test-some/with space_and_underscore")).isTrue();

    List<Set<String>> specialCharBatches = batchMetricsToCollect.get(METRIC_NAME_SPECIAL_CHARS);
    assertThat(specialCharBatches).hasSize(1);
    assertThat(specialCharBatches.get(0).contains("WebTransaction/special char %?name=s1&value=v1")).isTrue();
    assertThat(specialCharBatches.get(0).contains("WebTransaction/special char %?name=s2&value=v2")).isTrue();
  }

  @Test(expected = WingsException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUrlNotEndingWithSlash() {
    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder().url("thisisagoodtestURL").baseUrl("Thisbase/url/doesnot/endwith").build();
    newRelicService.validateAPMConfig(null, config);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUrlBeginingWithSlash() {
    APMValidateCollectorConfig config = APMValidateCollectorConfig.builder()
                                            .url("/thisisagoodtestURL")
                                            .baseUrl("Thisbase/url/doesnot/endwith/")
                                            .build();
    newRelicService.validateAPMConfig(null, config);
  }
}
