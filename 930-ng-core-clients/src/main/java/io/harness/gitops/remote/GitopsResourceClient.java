package io.harness.gitops.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.template.TemplateSummaryResponseDTO;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitopsResourceClient {

    @POST("agents")
    Call<ResponseDTO<PageResponse<String>>> listAgents(
            @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
            @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
            @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
            @Query(value = NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
            @Body Object filter);

    @POST("applications")
    Call<ResponseDTO<PageResponse<String>>> listApps(
            @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
            @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
            @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
            @Query(value = NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
            @Body Object filter);

    @POST("repositories")
    Call<ResponseDTO<PageResponse<String>>>  listRepositories(
            @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
            @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
            @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
            @Query(value = NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
            @Body Object filter);

    @POST("clusters")
    Call<ResponseDTO<PageResponse<String>>>  listClusters(
            @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
            @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
            @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
            @Query(value = NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
            @Body Object filter);
}
