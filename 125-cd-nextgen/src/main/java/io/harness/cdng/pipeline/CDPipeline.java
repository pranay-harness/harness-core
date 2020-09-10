package io.harness.cdng.pipeline;

import io.harness.cdng.pipeline.CDPipeline.CDPipelineKeys;
import io.harness.cdng.visitor.helpers.cdpipeline.CDPipelineVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.mongo.index.Field;
import io.harness.ng.RsqlQueryable;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.Tag;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;

import java.util.List;

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
  @EntityName String name;
  @EntityIdentifier String identifier;
  String description;
  List<Tag> tags;
  @Singular List<StageElementWrapper> stages;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    stages.forEach(stage -> visitableChildren.add("stages", stage));
    return visitableChildren;
  }
}
