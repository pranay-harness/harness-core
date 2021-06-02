package io.harness.pipeline.remote;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.inputset.MergeInputSetResponseDTOPMS;
import io.harness.pms.inputset.MergeInputSetTemplateRequestDTO;
import io.harness.pms.pipeline.PMSPipelineSummaryResponseDTO;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;

import java.util.List;
import javax.ws.rs.DefaultValue;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@TargetModule(HarnessModule._888_PMS_CLIENT)
@OwnedBy(PIPELINE)
public interface PipelineServiceClient {
  String PIPELINE_ENDPOINT = "pipelines/";
  String PIPELINE_INPUT_SET_ENDPOINT = "inputSets/";

  @POST(PIPELINE_ENDPOINT + "list/")
  Call<ResponseDTO<PageResponse<PMSPipelineSummaryResponseDTO>>> listPipelines(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("page") @DefaultValue("0") int page, @Query("size") @DefaultValue("25") int size,
      @Query("sort") List<String> sort, @Query(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @Query("module") String module, @Query("filterIdentifier") String filterIdentifier,
      @Body PipelineFilterPropertiesDto filterProperties);

  @POST(PIPELINE_INPUT_SET_ENDPOINT + "mergeWithTemplateYaml/")
  Call<ResponseDTO<MergeInputSetResponseDTOPMS>> getMergeInputSetFromPipelineTemplate(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(value = NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @Query("pipelineBranch") String pipelineBranch, @Query("pipelineRepoID") String pipelineRepoID,
      @Query("branch") String branch, @Query("repoIdentifier") String yamlGitConfigId,
      @Query("getDefaultFromOtherRepo") Boolean defaultFromOtherRepo,
      @Body MergeInputSetTemplateRequestDTO mergeInputSetTemplateRequestDTO);
}
