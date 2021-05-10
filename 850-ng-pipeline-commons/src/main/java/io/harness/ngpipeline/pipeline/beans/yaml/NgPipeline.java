package io.harness.ngpipeline.pipeline.beans.yaml;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.beans.common.SwaggerConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.mongo.index.Field;
import io.harness.ng.RsqlQueryable;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline.NgPipelineKeys;
import io.harness.ngpipeline.visitor.helpers.ngpipeline.NgPipelineVisitorHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.walktree.visitor.validation.annotations.Required;
import io.harness.walktree.visitor.validation.modes.PostInputSet;
import io.harness.walktree.visitor.validation.modes.PreInputSet;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NgPipelineKeys")
@RsqlQueryable(fields =
    {
      @Field(NgPipelineKeys.name)
      , @Field(NgPipelineKeys.identifier), @Field(NgPipelineKeys.description), @Field(NgPipelineKeys.tags),
          @Field(NgPipelineKeys.stages)
    })
@SimpleVisitorHelper(helperClass = NgPipelineVisitorHelper.class)
@TypeAlias("ngPipeline")
@OwnedBy(PIPELINE)
@ToBeDeleted
@Deprecated
public class NgPipeline implements Pipeline, Visitable {
  @NotNull(groups = PreInputSet.class) @Required(groups = PostInputSet.class) @EntityName String name;
  @NotNull(groups = PreInputSet.class) @Required(groups = PostInputSet.class) @EntityIdentifier String identifier;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;
  Map<String, String> tags;

  List<NGVariable> variables;
  private CodeBase ciCodebase;

  @Singular List<StageElementWrapper> stages;
  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(variable -> visitableChildren.add("variables", variable));
    }
    if (EmptyPredicate.isNotEmpty(stages)) {
      stages.forEach(stage -> visitableChildren.add("stages", stage));
    }
    return visitableChildren;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(identifier).build();
  }
}
