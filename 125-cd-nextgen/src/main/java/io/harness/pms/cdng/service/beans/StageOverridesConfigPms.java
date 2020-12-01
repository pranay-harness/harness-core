package io.harness.pms.cdng.service.beans;

import io.harness.beans.ParameterField;
import io.harness.common.SwaggerConstants;
import io.harness.pms.cdng.artifact.bean.yaml.ArtifactListConfigPms;
import io.harness.pms.cdng.manifest.yaml.ManifestConfigWrapperPms;
import io.harness.pms.yaml.core.variables.NGVariablePms;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stageOverridesConfigPms")
public class StageOverridesConfigPms {
  String uuid;
  List<NGVariablePms> variables;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useVariableOverrideSets;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useArtifactOverrideSets;
  ArtifactListConfigPms artifacts;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> useManifestOverrideSets;
  List<ManifestConfigWrapperPms> manifests;
}
