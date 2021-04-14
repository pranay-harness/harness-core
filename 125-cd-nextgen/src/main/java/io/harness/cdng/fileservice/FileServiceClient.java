package io.harness.cdng.fileservice;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileBucket;
import io.harness.rest.RestResponse;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import retrofit2.Call;
import retrofit2.http.GET;

@OwnedBy(CDP)
public interface FileServiceClient {
  String FILE_SERVICE_API = "ng/file-service";

  @GET(FILE_SERVICE_API + "/latestFileId")
  Call<RestResponse<String>> getLatestFileId(
      @QueryParam("entityId") @NotNull String entityId, @QueryParam("fileBucket") @NotNull FileBucket fileBucket);
}
