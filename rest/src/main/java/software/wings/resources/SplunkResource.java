package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.impl.splunk.SplunkLogElement;
import software.wings.service.intfc.splunk.SplunkService;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 */
@Api("splunk")
@Path("/splunk")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class SplunkResource {
  @Inject private SplunkService splunkService;

  @POST
  @Path("/save-logs")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveSplunkLogData(@QueryParam("accountId") String accountId,
      @QueryParam("appId") final String appId, List<SplunkLogElement> logData) throws IOException {
    return new RestResponse<>(splunkService.saveLogData(appId, logData));
  }
}
