package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.service.intfc.servicenow.ServiceNowService;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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

  /**
   * List.
   *
   * @param accountId the account id
   * @param appId the account id
   * @return the rest response
   */
  @GET
  @Path("ticket-types")
  @Timed
  @ExceptionMetered
  public RestResponse getTicketTypes(
      @QueryParam("appId") String appId, @QueryParam("accountId") @NotEmpty String accountId) {
    List<ServiceNowTicketTypeDTO> response = new ArrayList<>();
    for (ServiceNowTicketType ticketType : ServiceNowTicketType.values()) {
      response.add(new ServiceNowTicketTypeDTO(ticketType.name(), ticketType.getDisplayName()));
    }
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
}
