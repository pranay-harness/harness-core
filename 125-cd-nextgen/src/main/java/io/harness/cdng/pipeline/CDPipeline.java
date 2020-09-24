package io.harness.cdng.pipeline;

import io.harness.beans.ParameterField;
import io.harness.cdng.pipeline.CDPipeline.CDPipelineKeys;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.cdpipeline.CDPipelineVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.mongo.index.Field;
import io.harness.ng.RsqlQueryable;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.walktree.visitor.validation.annotations.Required;
import io.harness.walktree.visitor.validation.modes.PostInputSet;
import io.harness.walktree.visitor.validation.modes.PreInputSet;
import io.harness.yaml.core.Tag;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CDPipelineKeys")
@RsqlQueryable(fields =
    {
      @Field(CDPipelineKeys.name)
      , @Field(CDPipelineKeys.identifier), @Field(CDPipelineKeys.description), @Field(CDPipelineKeys.tags),
          @Field(CDPipelineKeys.stages)
    })
@SimpleVisitorHelper(helperClass = CDPipelineVisitorHelper.class)
public class CDPipeline implements Pipeline, Visitable {
  @NotNull(groups = PreInputSet.class) @Required(groups = PostInputSet.class) @EntityName String name;
  @NotNull(groups = PreInputSet.class) @Required(groups = PostInputSet.class) @EntityIdentifier String identifier;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;
  List<Tag> tags;
  @Singular List<StageElementWrapper> stages;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    stages.forEach(stage -> visitableChildren.add("stages", stage));
    return visitableChildren;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.CD_PIPELINE).build();
  }
}
