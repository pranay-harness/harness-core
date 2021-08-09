package io.harness.modules.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface ModulesClient {
  String MODULES_API = "ng/modules";

  @GET(MODULES_API)
  Call<RestResponse<List<ModuleType>>> getEnabledModules(@Query(value = "accountId") String accountId);
}
