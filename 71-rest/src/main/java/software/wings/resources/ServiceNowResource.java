package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.service.intfc.servicenow.ServiceNowService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("servicenow")
@Path("/servicenow")
@Produces("application/json")
public class ServiceNowResource {
  @Inject ServiceNowService serviceNowService;

  @Data
  @AllArgsConstructor
  private class ServiceNowTicketTypeDTO {
    String key;
    String name;
  }

  @GET
  @Path("ticket-types")
  @Timed
  @ExceptionMetered
  public RestResponse getTicketTypes(
      @QueryParam("appId") String appId, @QueryParam("accountId") @NotEmpty String accountId) {
    List<ServiceNowTicketTypeDTO> response;
    response = Arrays.stream(ServiceNowTicketType.values())
                   .map(ticketType -> new ServiceNowTicketTypeDTO(ticketType.name(), ticketType.getDisplayName()))
                   .collect(Collectors.toList());
    return new RestResponse<>(response);
  }

  @GET
  @Path("{connectorId}/states")
  @Timed
  @ExceptionMetered
  public RestResponse getStates(@QueryParam("appId") String appId, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("ticketType") ServiceNowTicketType ticketType, @PathParam("connectorId") String connectorId) {
    return new RestResponse<>(serviceNowService.getStates(ticketType, accountId, connectorId, appId));
  }

  @GET
  @Path("{connectorId}/approvalstates")
  @Timed
  @ExceptionMetered
  public RestResponse getApprovalValues(@QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("ticketType") ServiceNowTicketType ticketType,
      @PathParam("connectorId") String connectorId) {
    return new RestResponse<>(serviceNowService.getApprovalValues(ticketType, accountId, connectorId, appId));
  }

  @GET
  @Path("{connectorId}/createMeta")
  @Timed
  @ExceptionMetered
  public RestResponse getCreateMeta(@QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("ticketType") ServiceNowTicketType ticketType,
      @PathParam("connectorId") String connectorId) {
    return new RestResponse<>(serviceNowService.getCreateMeta(ticketType, accountId, connectorId, appId));
  }

  @GET
  @Path("{connectorId}/additionalFields")
  @Timed
  @ExceptionMetered
  public RestResponse getAdditionalFields(@QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("ticketType") ServiceNowTicketType ticketType,
      @PathParam("connectorId") String connectorId) {
    return new RestResponse<>(serviceNowService.getAdditionalFields(ticketType, accountId, connectorId, appId));
  }
}
