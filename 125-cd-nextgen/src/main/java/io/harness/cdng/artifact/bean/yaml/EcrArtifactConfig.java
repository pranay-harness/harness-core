package io.harness.cdng.artifact.bean.yaml;

import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.ECR_NAME;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
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
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ECR_NAME)
@TypeAlias("ecrArtifactConfig")
public class EcrArtifactConfig implements ArtifactConfig {
  /**
   * AWS connector to connect to Google Container Registry.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> connectorRef;
  /**
   * Region in which the artifact source is located.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> region;
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
  /**
   * Whether this config corresponds to primary artifact.
   */
  boolean isPrimaryArtifact;

  @Override
  public ArtifactSourceType getSourceType() {
    return ArtifactSourceType.ECR;
  }

  @Override
  public String getUniqueHash() {
    return null;
  }

  @Override
  public boolean isPrimaryArtifact() {
    return false;
  }

  @Override
  public void setPrimaryArtifact(boolean primaryArtifact) {}

  @Override
  public void setIdentifier(String identifier) {}

  @Override
  public String getIdentifier() {
    return null;
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    return null;
  }
}
