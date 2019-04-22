package software.wings.resources;

import com.google.inject.Inject;

import io.harness.event.lite.FeatureAvailability;
import io.harness.event.lite.FeatureAvailabilityProviderService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("features")
@Path("/features")
@Produces("application/json")
public class LiteFeatureAvailabilityResource {
  @Inject private FeatureAvailabilityProviderService featureAvailabilityService;

  @GET
  public RestResponse<List<FeatureAvailability>> getFeatureAvailability(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(featureAvailabilityService.listFeatureAvailability(accountId));
  }
}
