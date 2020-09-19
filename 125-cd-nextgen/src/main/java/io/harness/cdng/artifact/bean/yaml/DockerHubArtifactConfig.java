package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ParameterField;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.visitor.helpers.artifact.DockerHubArtifactConfigVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

import java.util.Arrays;
import java.util.List;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ArtifactSourceConstants.DOCKER_HUB_NAME)
@SimpleVisitorHelper(helperClass = DockerHubArtifactConfigVisitorHelper.class)
public class DockerHubArtifactConfig implements ArtifactConfig, Visitable {
  /**
   * Docker hub registry connector.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> dockerhubConnector;
  /**
   * Images in repos need to be referenced via a path.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> imagePath;
  /**
   * Tag refers to exact tag number.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> tag;
  /**
   * Tag regex is used to get latest build from builds matching regex.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> tagRegex;
  /**
   * Identifier for artifact.
   */
  @EntityIdentifier String identifier;
  /** Whether this config corresponds to primary artifact.*/
  boolean primaryArtifact;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public ArtifactSourceType getSourceType() {
    return ArtifactSourceType.DOCKER_HUB;
  }

  @Override
  public String getUniqueHash() {
    List<String> valuesList = Arrays.asList(dockerhubConnector.getValue(), imagePath.getValue());
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) overrideConfig;
    DockerHubArtifactConfig resultantConfig = this;
    if (dockerHubArtifactConfig.getDockerhubConnector() != null) {
      resultantConfig = resultantConfig.withDockerhubConnector(dockerHubArtifactConfig.getDockerhubConnector());
    }
    if (dockerHubArtifactConfig.getImagePath() != null) {
      resultantConfig = resultantConfig.withImagePath(dockerHubArtifactConfig.getImagePath());
    }
    if (dockerHubArtifactConfig.getTag() != null) {
      resultantConfig = resultantConfig.withTag(dockerHubArtifactConfig.getTag());
    }
    if (dockerHubArtifactConfig.getTagRegex() != null) {
      resultantConfig = resultantConfig.withTagRegex(dockerHubArtifactConfig.getTagRegex());
    }
    return resultantConfig;
  }
}
