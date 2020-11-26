package io.harness.cdng.artifact.bean.yaml;

import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.DOCKER_HUB_NAME;

import io.harness.beans.ParameterField;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.artifact.DockerHubArtifactConfigVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(DOCKER_HUB_NAME)
@SimpleVisitorHelper(helperClass = DockerHubArtifactConfigVisitorHelper.class)
@TypeAlias("dockerHubArtifactConfig")
public class DockerHubArtifactConfig implements ArtifactConfig, Visitable {
  /**
   * Docker hub registry connector.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> connectorRef;
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
    List<String> valuesList = Arrays.asList(connectorRef.getValue(), imagePath.getValue());
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) overrideConfig;
    DockerHubArtifactConfig resultantConfig = this;
    if (dockerHubArtifactConfig.getConnectorRef() != null) {
      resultantConfig = resultantConfig.withConnectorRef(dockerHubArtifactConfig.getConnectorRef());
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

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SPEC).build();
  }
}
