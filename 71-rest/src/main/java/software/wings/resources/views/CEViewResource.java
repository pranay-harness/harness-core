package software.wings.resources.views;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.service.CEViewService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("view")
@Path("/view")
@Produces("application/json")
public class CEViewResource {
  private CEViewService ceViewService;

  @Inject
  public CEViewResource(CEViewService ceViewService) {
    this.ceViewService = ceViewService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> saveCustomField(@QueryParam("accountId") String accountId, CEView ceView) {
    return new RestResponse<>(ceViewService.save(ceView));
  }
}
