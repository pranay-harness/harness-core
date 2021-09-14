/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.webhook.services.impl;

import static io.harness.rule.OwnerRule.HARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.net.MalformedURLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class WebhookServiceImplTest extends CategoryTest {
  @InjectMocks @Spy WebhookServiceImpl webhookService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void getTargetUrlTest() throws MalformedURLException {
    doReturn("https://app.harness.io/gateway/ng/api/").when(webhookService).getWebhookBaseUrl();
    final String targetUrl = webhookService.getTargetUrl("abcde");
    assertThat(targetUrl).isEqualTo("https://app.harness.io/gateway/ng/api/webhook?accountIdentifier=abcde");

    doReturn("https://app.harness.io/gateway/ng/api").when(webhookService).getWebhookBaseUrl();
    final String targetUrl2 = webhookService.getTargetUrl("abcde");
    assertThat(targetUrl2).isEqualTo("https://app.harness.io/gateway/ng/api/webhook?accountIdentifier=abcde");
  }
}
