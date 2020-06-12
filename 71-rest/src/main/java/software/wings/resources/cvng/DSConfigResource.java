package software.wings.resources.cvng;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.DSConfig;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cv-nextgen/ds-config/")
@Path("/cv-nextgen/ds-config")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class DSConfigResource {
  @Inject private DSConfigService dsConfigService;
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<DSConfig>> getDataSourceCVConfigs(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("productName") String productName) {
    return new RestResponse<>(dsConfigService.list(accountId, connectorId, productName));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public void saveDataSourceCVConfig(@QueryParam("accountId") @Valid final String accountId, @Body DSConfig dsConfig) {
    dsConfigService.upsert(dsConfig);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public void deleteByGroup(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("productName") String productName,
      @QueryParam("identifier") String identifier) {
    dsConfigService.delete(accountId, connectorId, productName, identifier);
  }
}