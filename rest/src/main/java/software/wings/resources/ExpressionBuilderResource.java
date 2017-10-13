package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.apache.commons.lang.StringUtils;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
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
@AuthRule(APPLICATION)
public class ExpressionBuilderResource {
  @Inject private ExpressionBuilderService expressionBuilderService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> listExpressions(@QueryParam("appId") String appId,
      @QueryParam("entityId") String entityId, @QueryParam("entityType") EntityType entityType,
      @QueryParam("serviceId") String serviceId, @QueryParam("stateType") String strStateType) {
    StateType stateType = null;
    if (!StringUtils.isBlank(strStateType)) {
      try {
        if (!strStateType.contentEquals("\"\"")) {
          stateType = StateType.valueOf(strStateType);
        }
      } catch (IllegalArgumentException e) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid state type " + strStateType);
      }
    }
    return new RestResponse(
        expressionBuilderService.listExpressions(appId, entityId, entityType, serviceId, stateType));
  }
}
