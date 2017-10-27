package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 10/30/17.
 */
@Api("secrets")
@Path("/secrets")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class SecretManagementResource {
  @Inject private SecretManager secretManager;

  @GET
  @Path("/usage")
  @Timed
  @ExceptionMetered
  RestResponse<List<SecretUsageLog>> getUsageLogs(@QueryParam("accountId") final String accountId,
      @QueryParam("entityId") final String entityId, @QueryParam("type") final SettingVariableTypes variableType)
      throws IllegalAccessException {
    return new RestResponse<>(secretManager.getUsageLogs(entityId, variableType));
  }

  @GET
  @Path("/change-logs")
  @Timed
  @ExceptionMetered
  RestResponse<List<Pair<Long, EmbeddedUser>>> getChangeLogs(@QueryParam("accountId") final String accountId,
      @QueryParam("entityId") final String entityId, @QueryParam("type") final SettingVariableTypes variableType)
      throws IllegalAccessException {
    return new RestResponse<>(secretManager.getChangeLogs(entityId, variableType));
  }
}
