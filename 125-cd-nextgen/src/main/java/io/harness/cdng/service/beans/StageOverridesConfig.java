package io.harness.cdng.service.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.StageOverridesVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@SimpleVisitorHelper(helperClass = StageOverridesVisitorHelper.class)
@TypeAlias("stageOverridesConfig")
@OwnedBy(CDC)
public class StageOverridesConfig implements Visitable {
  List<NGVariable> variables;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useVariableOverrideSets;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useArtifactOverrideSets;
  ArtifactListConfig artifacts;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useManifestOverrideSets;
  List<ManifestConfigWrapper> manifests;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();

    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }
    children.add("artifacts", artifacts);
    if (EmptyPredicate.isNotEmpty(manifests)) {
      manifests.forEach(manifest -> children.add("manifests", manifest));
    }
    return children;
  }
}
