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
  String UPDATE_SETTINGS_ATTRIBUTE = "/api/ng/settings/{attrId}";
  String SAVE_SETTINGS_ATTRIBUTE = "/api/ng/settings";
  String VALIDATE_SETTINGS_ATTRIBUTE = "/api/ng/settings/validateName";
  String VALIDATE_CONNECTIVITY = "/api/ng/settings/validate-connectivity";
  String GET_SETTING_ATTRIBUTE = "/api/ng/settings/{attrId}";
  String DELETE_SETTING_ATTRIBUTE = "/api/ng/settings/{attrId}";

  @POST(SAVE_SETTINGS_ATTRIBUTE)
  Call<RestResponse<SettingAttribute>> saveSmtpSettings(
      @Query("appId") String appId, @Query("accountId") String accountId, @Body SettingAttribute variable);

  @POST(VALIDATE_SETTINGS_ATTRIBUTE)
  Call<RestResponse<Boolean>> validateSmtpSettings(@Query("name") String name, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("envId") String envId);

  @PUT(UPDATE_SETTINGS_ATTRIBUTE)
  Call<RestResponse<SettingAttribute>> updateSmtpSettings(
      @Path("attrId") String accountId, @Query("appId") String appId, @Body SettingAttribute variable);

  @DELETE(DELETE_SETTING_ATTRIBUTE)
  Call<RestResponse<Boolean>> deleteSmtpSettings(@Path("attrId") String attrId, @Query("appId") String appId);

  @GET(GET_SETTING_ATTRIBUTE)
  Call<RestResponse<SettingAttribute>> getSmtpSettings(@Path("attrId") String attrId, @Query("appId") String appId);

  @POST(VALIDATE_CONNECTIVITY)
  Call<RestResponse<ValidationResult>> validateConnectivitySmtpSettings(@Query("appId") String appId,
      @Query("accountId") String accountId, @Query("to") String to, @Query("subject") String subject,
      @Query("body") String body, @Body SettingAttribute variable);
}
