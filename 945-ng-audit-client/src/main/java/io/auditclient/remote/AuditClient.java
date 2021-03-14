package io.harness.auditclient.remote;

import io.harness.audit.beans.AuditEventDTO;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuditClient {
  String AUDIT_SAVE_API = "ng/audits";

  @POST(AUDIT_SAVE_API) Call<RestResponse<AuditEventDTO>> createAudit(@Body AuditEventDTO auditEventDTO);
}