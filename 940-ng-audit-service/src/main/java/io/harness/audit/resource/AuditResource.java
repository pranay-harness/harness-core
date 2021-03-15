package io.harness.audit.resource;

import static io.harness.audit.mapper.AuditEventMapper.toDTO;

import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;

import com.google.inject.Inject;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.POST;
import retrofit2.http.Body;

public class AuditResource {
  @Inject private AuditService auditService;

  @POST
  @ApiOperation(hidden = true, value = "Create Audit")
  @InternalApi
  public ResponseDTO<AuditEventDTO> create(@Body AuditEventDTO auditEventDTO) {
    return ResponseDTO.newResponse(toDTO(auditService.create(auditEventDTO)));
  }
}
