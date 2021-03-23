package io.harness.audit.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.mapper.AuditEventMapper.toDTO;
import static io.harness.beans.SortOrder.OrderType.DESC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.entities.AuditEvent.AuditEventKeys;
import io.harness.audit.mapper.AuditEventMapper;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
@Api("audits")
@Path("audits")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class AuditResource {
  @Inject private final AuditService auditService;

  @POST
  @ApiOperation(hidden = true, value = "Create an Audit", nickname = "postAudit")
  @InternalApi
  public ResponseDTO<AuditEventDTO> create(@Valid AuditEventDTO auditEventDTO) {
    return ResponseDTO.newResponse(toDTO(auditService.create(auditEventDTO)));
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "Get Audit list", nickname = "getAuditList")
  public ResponseDTO<PageResponse<AuditEventDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @BeanParam PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField(AuditEventKeys.timestamp, DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Page<AuditEventDTO> audits =
        auditService.list(accountIdentifier, pageRequest, auditFilterPropertiesDTO).map(AuditEventMapper::toDTO);
    return ResponseDTO.newResponse(getNGPageResponse(audits));
  }
}
