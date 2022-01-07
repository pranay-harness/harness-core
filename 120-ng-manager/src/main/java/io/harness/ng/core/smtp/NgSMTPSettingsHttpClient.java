/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.smtp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface NgSMTPSettingsHttpClient {
  String UPDATE_SETTINGS_ATTRIBUTE = "ng/settings/{attrId}";
  String SAVE_SETTINGS_ATTRIBUTE = "ng/settings";
  String VALIDATE_SETTINGS_ATTRIBUTE = "ng/settings/validateName";
  String VALIDATE_CONNECTIVITY = "ng/settings/validate-connectivity";
  String GET_SETTING_ATTRIBUTE = "ng/settings";
  String DELETE_SETTING_ATTRIBUTE = "ng/settings/{attrId}";

  @POST(SAVE_SETTINGS_ATTRIBUTE)
  Call<RestResponse<SettingAttribute>> saveSmtpSettings(
      @Query("appId") String appId, @Query("accountId") String accountId, @Body SettingAttribute variable);

  @POST(VALIDATE_SETTINGS_ATTRIBUTE)
  Call<RestResponse<Boolean>> validateSmtpSettings(@Query("name") String name, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("envId") String envId);

  @PUT(UPDATE_SETTINGS_ATTRIBUTE)
  Call<RestResponse<SettingAttribute>> updateSmtpSettings(
      @Path("attrId") String attrId, @Query("appId") String appId, @Body SettingAttribute variable);

  @DELETE(DELETE_SETTING_ATTRIBUTE)
  Call<RestResponse<Boolean>> deleteSmtpSettings(@Path("attrId") String attrId, @Query("appId") String appId);

  @GET(GET_SETTING_ATTRIBUTE)
  Call<RestResponse<SettingAttribute>> getSmtpSettings(@Query("accountId") String accountId);

  @POST(VALIDATE_CONNECTIVITY)
  Call<RestResponse<ValidationResult>> validateConnectivitySmtpSettings(@Query("attrId") String attrId,
      @Query("appId") String appId, @Query("accountId") String accountId, @Query("to") String to,
      @Query("subject") String subject, @Query("body") String body);
}
