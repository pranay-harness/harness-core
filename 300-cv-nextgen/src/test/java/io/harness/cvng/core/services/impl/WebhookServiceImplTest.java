/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.WebhookToken;
import io.harness.cvng.core.entities.WebhookToken.WebhookTokenKeys;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.exception.CVWebhookException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookServiceImplTest extends CvNextGenTestBase {
  @Inject HPersistence hPersistence;
  @Inject WebhookService webhookService;

  private String projectIdentifier;
  private String orgIdentifier;
  private String accountId;

  @Before
  public void setUp() throws Exception {
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateWebhookToken_valid() {
    WebhookToken token = WebhookToken.builder()
                             .token("testsampletoken123")
                             .projectIdentifier(projectIdentifier)
                             .orgIdentifier(orgIdentifier)
                             .build();
    hPersistence.save(token);

    boolean isValid = webhookService.validateWebhookToken("testsampletoken123", projectIdentifier, orgIdentifier);
    assertThat(isValid).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateWebhookToken_invalid() {
    WebhookToken token = WebhookToken.builder()
                             .token("testsampletoken123")
                             .projectIdentifier(projectIdentifier)
                             .orgIdentifier(orgIdentifier)
                             .build();
    hPersistence.save(token);

    boolean isValid = webhookService.validateWebhookToken("incorrectToken", projectIdentifier, orgIdentifier);
    assertThat(isValid).isFalse();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateWebhookToken_noTokenExists() {
    boolean isValid = webhookService.validateWebhookToken("incorrectToken", projectIdentifier, orgIdentifier);
    assertThat(isValid).isFalse();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateWebhookToken() {
    List<WebhookToken> webhookToken = hPersistence.createQuery(WebhookToken.class)
                                          .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                                          .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier)
                                          .asList();
    assertThat(webhookToken).isNullOrEmpty();

    String token = webhookService.createWebhookToken(projectIdentifier, orgIdentifier);
    webhookToken = hPersistence.createQuery(WebhookToken.class)
                       .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                       .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier)
                       .asList();
    assertThat(webhookToken).isNotNull();
    assertThat(webhookToken.size()).isEqualTo(1);
    assertThat(token).isNotNull();
    assertThat(token).isEqualTo(webhookToken.get(0).getToken());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateWebhookToken_alreadyExists() {
    WebhookToken existingToken = WebhookToken.builder()
                                     .token("testsampletoken123")
                                     .projectIdentifier(projectIdentifier)
                                     .orgIdentifier(orgIdentifier)
                                     .build();
    hPersistence.save(existingToken);

    String token = webhookService.createWebhookToken(projectIdentifier, orgIdentifier);
    assertThat(token).isNotNull();
    assertThat(token).isEqualTo(existingToken.getToken());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testReCreateWebhookToken() {
    WebhookToken existingToken = WebhookToken.builder()
                                     .token("testsampletoken123")
                                     .projectIdentifier(projectIdentifier)
                                     .orgIdentifier(orgIdentifier)
                                     .build();
    hPersistence.save(existingToken);

    String token = webhookService.recreateWebhookToken(projectIdentifier, orgIdentifier);
    assertThat(token).isNotNull();
    assertThat(token).isNotEqualTo(existingToken.getToken());
  }

  @Test(expected = CVWebhookException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testReCreateWebhookToken_noPreviousWebhook() {
    List<WebhookToken> webhookToken = hPersistence.createQuery(WebhookToken.class)
                                          .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                                          .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier)
                                          .asList();
    assertThat(webhookToken).isNullOrEmpty();

    String token = webhookService.recreateWebhookToken(projectIdentifier, orgIdentifier);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testDeleteWebhookToken() {
    WebhookToken existingToken = WebhookToken.builder()
                                     .token("testsampletoken123")
                                     .projectIdentifier(projectIdentifier)
                                     .orgIdentifier(orgIdentifier)
                                     .build();
    hPersistence.save(existingToken);

    webhookService.deleteWebhookToken(projectIdentifier, orgIdentifier);
    List<WebhookToken> webhookToken = hPersistence.createQuery(WebhookToken.class)
                                          .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                                          .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier)
                                          .asList();

    assertThat(webhookToken).isNullOrEmpty();
  }
}
