package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.sm.states.DatadogState;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("datadog")
@Path("/datadog")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class DatadogResource {
  @GET
  @Path("/metric-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<DatadogState.Metric>> getAllMetricNames(@QueryParam("accountId") String accountId)
      throws IOException {
    return new RestResponse<>(DatadogState.metricNames());
  }
}
