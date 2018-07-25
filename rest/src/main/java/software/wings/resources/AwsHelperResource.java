package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.NameValuePair;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
@Scope(SETTING)
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
  @Deprecated
  public RestResponse<Map<String, String>> list(@QueryParam("accountId") String accountId) {
    return new RestResponse(awsHelperResourceService.getRegions());
  }

  /**
   * List.
   *
   * @param accountId                the account id
   * @return the rest response
   */
  @GET
  @Path("/aws-regions")
  @Timed
  @ExceptionMetered
  @Deprecated
  public RestResponse<List<NameValuePair>> listAwsRegions(@QueryParam("accountId") String accountId) {
    return new RestResponse(awsHelperResourceService.getAwsRegions());
  }

  @GET
  @Path("tags")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> listTags(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @QueryParam("computeProviderId") String computeProviderId, @QueryParam("resourceType") String resourceType) {
    return new RestResponse<>(awsHelperResourceService.listTags(appId, computeProviderId, region, resourceType));
  }
}
