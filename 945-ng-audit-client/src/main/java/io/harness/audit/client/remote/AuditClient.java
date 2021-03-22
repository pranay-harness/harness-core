package io.harness.audit.client.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@OwnedBy(PL)
public interface AuditClient {
  String AUDITS_API = "audits";

  @POST(AUDITS_API) Call<RestResponse<AuditEventDTO>> createAudit(@Body AuditEventDTO auditEventDTO);
}
