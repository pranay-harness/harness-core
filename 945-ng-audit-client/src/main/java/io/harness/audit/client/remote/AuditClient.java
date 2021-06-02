package io.harness.audit.client.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@OwnedBy(PL)
public interface AuditClient {
  String AUDITS_API = "audits";

  @POST(AUDITS_API) Call<ResponseDTO<Boolean>> createAudit(@Body AuditEventDTO auditEventDTO);
}
