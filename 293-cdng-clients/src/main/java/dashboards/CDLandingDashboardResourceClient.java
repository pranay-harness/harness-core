package dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.DeploymentStatsSummary;
import io.harness.dashboards.EnvCount;
import io.harness.dashboards.GroupBy;
import io.harness.dashboards.ProjectsDashboardInfo;
import io.harness.dashboards.ServicesCount;
import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PIPELINE)
public interface CDLandingDashboardResourceClient {
  String LANDING_DASHBOARDS_API = "landingDashboards";

  @GET(LANDING_DASHBOARDS_API + "/activeServices")
  Call<ResponseDTO<ServicesDashboardInfo>> get(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval, @NotNull @Query("sortBy") SortBy sortBy);

  @GET(LANDING_DASHBOARDS_API + "/topProjects")
  Call<ResponseDTO<ProjectsDashboardInfo>> getTopProjects(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval);

  @GET(LANDING_DASHBOARDS_API + "/deploymentStatsSummary")
  Call<ResponseDTO<DeploymentStatsSummary>> getDeploymentStatsSummary(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval);

  @GET(LANDING_DASHBOARDS_API + "/timeDeploymentStats")
  Call<ResponseDTO<List<TimeBasedDeploymentInfo>>> getTimeWiseDeploymentInfo(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval,
      @NotNull @QueryParam("groupBy") GroupBy groupBy);

  @GET(LANDING_DASHBOARDS_API + "/servicesCount")
  Call<ResponseDTO<ServicesCount>> getServicesCount(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval);

  @GET(LANDING_DASHBOARDS_API + "/envCount")
  Call<ResponseDTO<EnvCount>> getEnvCount(@NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @Query(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @Query(NGResourceFilterConstants.END_TIME) long endInterval);
}
