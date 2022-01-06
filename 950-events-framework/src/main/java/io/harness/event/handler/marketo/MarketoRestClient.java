/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.handler.marketo;

import io.harness.event.model.marketo.Campaign;
import io.harness.event.model.marketo.GetLeadResponse;
import io.harness.event.model.marketo.LeadRequestWithEmail;
import io.harness.event.model.marketo.LeadRequestWithId;
import io.harness.event.model.marketo.LoginResponse;
import io.harness.event.model.marketo.Response;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MarketoRestClient {
  @GET("identity/oauth/token?grant_type=client_credentials")
  Call<LoginResponse> login(
      @Query(value = "client_id") String clientId, @Query(value = "client_secret") String clientSecret);

  @Headers("Accept: application/json")
  @POST("rest/v1/campaigns/{campaignId}/trigger.json")
  Call<Response> triggerCampaign(
      @Path("campaignId") long campaignId, @Query("access_token") String accessToken, @Body Campaign campaignRequest);

  @Headers("Accept: application/json")
  @POST("rest/v1/leads.json")
  Call<Response> updateLead(@Query("access_token") String accessToken, @Body LeadRequestWithId lead);

  @Headers("Accept: application/json")
  @POST("rest/v1/leads.json")
  Call<Response> createLead(@Query("access_token") String accessToken, @Body LeadRequestWithEmail lead);

  @Headers("Accept: application/json")
  @GET("rest/v1/leads.json")
  Call<GetLeadResponse> getLead(@Query("access_token") String accessToken, @Query("filterType") String filterType,
      @Query("filterValues") String filterValues);
}
