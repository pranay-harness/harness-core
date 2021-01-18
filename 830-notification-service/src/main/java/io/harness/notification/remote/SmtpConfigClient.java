package io.harness.notification.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface SmtpConfigClient {
  String SMTP_CONFIG_BASEURI = "ng/smtp-config";

  @GET(SMTP_CONFIG_BASEURI)
  @KryoRequest
  @KryoResponse
  Call<RestResponse<SmtpConfigResponse>> getSmtpConfig(@Query(value = "accountId") String accountId);
}
