/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

public interface WebhookService {
  boolean validateWebhookToken(String webhookToken, String projectIdentifier, String orgIdentifier);
  String createWebhookToken(String projectIdentifier, String orgIdentifier);
  String recreateWebhookToken(String projectIdentifier, String orgIdentifier);
  void deleteWebhookToken(String projectIdentifier, String orgIdentifier);
}
