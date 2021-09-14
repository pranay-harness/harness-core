/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.webhook.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@OwnedBy(DX)
public interface WebhookEventClient {
  String WEBHOOK_API = "webhookevent";

  @POST(WEBHOOK_API)
  Call<ResponseDTO<UpsertWebhookResponseDTO>> upsertWebhook(@Body UpsertWebhookRequestDTO upsertWebhookRequest);
}
