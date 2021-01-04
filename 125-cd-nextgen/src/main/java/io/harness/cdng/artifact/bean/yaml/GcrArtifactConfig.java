package io.harness.cdng.artifact.bean.yaml;

import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.GCR_NAME;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.artifact.GcrArtifactConfigVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.List;
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
@JsonTypeName(GCR_NAME)
@SimpleVisitorHelper(helperClass = GcrArtifactConfigVisitorHelper.class)
@TypeAlias("gcrArtifactConfig")
public class GcrArtifactConfig implements ArtifactConfig, Visitable {
  /**
   * GCP connector to connect to Google Container Registry.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> connectorRef;
  /**
   * Registry where the artifact source is located.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> registryHostname;
  /**
   * Images in repos need to be referenced via a path.
   */
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> imagePath;
  /**
   * Identifier for artifact.
   */
  @EntityIdentifier String identifier;
  /**
   * Whether this config corresponds to primary artifact.
   */
  boolean isPrimaryArtifact;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public ArtifactSourceType getSourceType() {
    return ArtifactSourceType.GCR;
  }

  @Override
  public String getUniqueHash() {
    List<String> valuesList = Arrays.asList(connectorRef.getValue(), registryHostname.getValue(), imagePath.getValue());
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    GcrArtifactConfig gcrArtifactSpecConfig = (GcrArtifactConfig) overrideConfig;
    GcrArtifactConfig resultantConfig = this;
    if (gcrArtifactSpecConfig.getConnectorRef() != null) {
      resultantConfig = resultantConfig.withConnectorRef(gcrArtifactSpecConfig.getConnectorRef());
    }
    if (gcrArtifactSpecConfig.getImagePath() != null) {
      resultantConfig = resultantConfig.withImagePath(gcrArtifactSpecConfig.getImagePath());
    }
    if (gcrArtifactSpecConfig.getRegistryHostname() != null) {
      resultantConfig = resultantConfig.withRegistryHostname(gcrArtifactSpecConfig.getRegistryHostname());
    }
    return resultantConfig;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SPEC).build();
  }
}
