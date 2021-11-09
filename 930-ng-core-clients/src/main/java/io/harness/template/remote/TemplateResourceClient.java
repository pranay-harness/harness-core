package io.harness.template.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.template.TemplateApplyRequestDTO;
import io.harness.ng.core.template.TemplateListType;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.ng.core.template.TemplateSummaryResponseDTO;
import io.harness.template.TemplateFilterPropertiesDTO;

import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.CDC)
public interface TemplateResourceClient {
  String TEMPLATE_ENDPOINT = "templates/";
  // list templates
  @POST(TEMPLATE_ENDPOINT + "list")
  Call<ResponseDTO<PageResponse<TemplateSummaryResponseDTO>>> listTemplates(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(value = "templateListType") TemplateListType templateListType,
      @Query(value = NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @Body TemplateFilterPropertiesDTO filterProperties);

  @POST(TEMPLATE_ENDPOINT + "applyTemplates")
  Call<ResponseDTO<TemplateMergeResponseDTO>> applyTemplatesOnGivenYaml(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body TemplateApplyRequestDTO templateApplyRequestDTO);
}
