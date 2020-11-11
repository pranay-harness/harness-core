package io.harness.pms.resources;

import com.google.inject.Inject;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.beans.BasicPipeline;
import io.harness.pms.entities.PipelineEntity;
import io.harness.pms.plan.common.yaml.YamlUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Api("/pipelines")
@Path("/pipelines")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))

public class PipelineResource {
  @POST
  @ApiOperation(value = "Create a Pipeline", nickname = "createPipeline")
  public Response createPipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) throws IOException {
    log.info("Creating pipeline");
    BasicPipeline basicPipeline = YamlUtils.read(yaml, BasicPipeline.class);
    PipelineEntity entity = PipelineEntity.builder()
                                .yaml(yaml)
                                .accountId(accountId)
                                .orgIdentifier(orgId)
                                .projectIdentifier(projectId)
                                .name(basicPipeline.getName())
                                .identifier(basicPipeline.getIdentifier())
                                .description(basicPipeline.getDescription())
                                .tags(TagMapper.convertToList(basicPipeline.getTags()))
                                .build();
    return Response.ok(entity.getUuid(), MediaType.APPLICATION_JSON_TYPE).build();
  }
}
