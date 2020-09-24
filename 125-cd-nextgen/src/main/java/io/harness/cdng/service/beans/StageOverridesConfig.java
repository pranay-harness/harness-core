package io.harness.cdng.service.beans;

import io.harness.beans.ParameterField;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.serviceconfig.StageOverridesVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@SimpleVisitorHelper(helperClass = StageOverridesVisitorHelper.class)
public class StageOverridesConfig implements Visitable {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useArtifactOverrideSets;
  ArtifactListConfig artifacts;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useManifestOverrideSets;
  List<ManifestConfigWrapper> manifests;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifacts", artifacts);
    manifests.forEach(manifest -> children.add("manifests", manifest));
    // add override sets if necessary
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.STAGE_OVERRIDES_CONFIG).build();
  }
}
