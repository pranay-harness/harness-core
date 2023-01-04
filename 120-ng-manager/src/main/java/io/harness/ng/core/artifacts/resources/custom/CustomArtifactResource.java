/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.artifacts.resources.custom;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.resources.custom.CustomResourceService;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.ng.core.artifacts.resources.util.ArtifactResourceUtils;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import software.wings.helpers.ext.jenkins.BuildDetails;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Api("artifacts")
@Path("/artifacts/custom")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class CustomArtifactResource {
  private final CustomResourceService customResourceService;
  private final ArtifactResourceUtils artifactResourceUtils;
  @POST
  @Path("builds")
  @Hidden
  @ApiOperation(value = "Gets Job details for Custom Artifact", nickname = "getJobDetailsForCustom")
  public ResponseDTO<List<BuildDetails>> getBuildsDetails(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @RequestBody(required = true, description = "Shell Script to fetch builds")
      CustomScriptInfo customScriptInfo, @NotNull @QueryParam("versionPath") String versionPath,
      @NotNull @QueryParam("arrayPath") String arrayPath, @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("fqnPath") String fqnPath, @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceRef) {
    List<BuildDetails> buildDetails =
        artifactResourceUtils.getCustomGetBuildDetails(arrayPath, versionPath, customScriptInfo, serviceRef, accountId,
            orgIdentifier, projectIdentifier, fqnPath, pipelineIdentifier, gitEntityBasicInfo);
    return ResponseDTO.newResponse(buildDetails);
  }
}
