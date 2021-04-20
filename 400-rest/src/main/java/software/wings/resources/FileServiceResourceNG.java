package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileBucket;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(value = "file-service", hidden = true)
@Path("/ng/file-service")
@Produces("application/json")
@Consumes("application/json")
// TODO: check with Vikas on Auth
@NextGenManagerAuth
@OwnedBy(CDP)
public class FileServiceResourceNG {
  @Inject private FileService fileService;

  @GET
  @Path("latestFileId")
  public RestResponse<String> getLatestFileId(
      @QueryParam("entityId") @NotNull String entityId, @QueryParam("fileBucket") @NotNull FileBucket fileBucket) {
    String latestFileId = fileService.getLatestFileId(entityId, fileBucket);
    return new RestResponse<>(latestFileId);
  }
}
