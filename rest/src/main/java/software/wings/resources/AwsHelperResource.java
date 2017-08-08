package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by sgurubelli on 7/16/17.
 */
@Api("awshelper")
@Path("/awshelper")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@AuthRule(SETTING)
public class AwsHelperResource {
  @Inject private AwsHelperResourceService awsHelperResourceService;

  /**
   * List.
   *
   * @param accountId                the account id
   * @return the rest response
   */
  @GET
  @Path("/regions")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> list(@QueryParam("accountId") String accountId) {
    return new RestResponse(awsHelperResourceService.getRegions());
  }
}
