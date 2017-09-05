package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.newrelic.NewRelicService;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 09/05/17.
 */
@Api("newrelic")
@Path("/newrelic")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class NewRelicResource {
  @Inject private NewRelicService newRelicService;

  @POST
  @Path("/save-metrics")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("applicationId") String applicationId, List<NewRelicMetricDataRecord> metricData) throws IOException {
    return new RestResponse<>(newRelicService.saveMetricData(accountId, applicationId, metricData));
  }
}
