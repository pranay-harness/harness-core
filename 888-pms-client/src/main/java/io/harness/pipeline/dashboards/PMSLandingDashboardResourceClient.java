package io.harness.pipeline.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.dashboards.PipelinesCount;

import java.util.List;
import javax.validation.constraints.NotNull;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PIPELINE)
public interface PMSLandingDashboardResourceClient {
  String LANDING_DASHBOARDS_ENDPOINT = "landingDashboards";

  @GET(LANDING_DASHBOARDS_ENDPOINT + "/pipelinesCount")
  Call<ResponseDTO<PipelinesCount>> getPipelinesCount(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<String> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval);
}
