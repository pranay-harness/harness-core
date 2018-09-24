package software.wings.resources;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.InvalidRequestException;
import io.swagger.annotations.Api;
import software.wings.beans.EntityType;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.sm.StateType;

import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by sgurubelli on 8/7/17.
 */
@Api("expression-builder")
@Path("/expression-builder")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
public class ExpressionBuilderResource {
  @Inject private ExpressionBuilderService expressionBuilderService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> listExpressions(@QueryParam("appId") String appId,
      @QueryParam("entityId") String entityId, @QueryParam("entityType") EntityType entityType,
      @QueryParam("serviceId") String serviceId, @QueryParam("stateType") String strStateType) {
    StateType stateType = null;
    if (isNotBlank(strStateType)) {
      try {
        if (!strStateType.contentEquals("\"\"")) {
          stateType = StateType.valueOf(strStateType);
        }
      } catch (IllegalArgumentException e) {
        throw new InvalidRequestException("Invalid state type " + strStateType);
      }
    }
    return new RestResponse(
        expressionBuilderService.listExpressions(appId, entityId, entityType, serviceId, stateType));
  }
}
