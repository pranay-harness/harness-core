package io.harness.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.visitor.helpers.manifest.ManifestConfigVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@NoArgsConstructor
@SimpleVisitorHelper(helperClass = ManifestConfigVisitorHelper.class)
@TypeAlias("manifestConfig")
public class ManifestConfig implements Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  @NotNull @EntityIdentifier String identifier;

  @NotNull @JsonProperty("type") ManifestConfigType type;

  @NotNull
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ManifestAttributes spec;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setSpec(ManifestAttributes spec) {
    this.spec = spec;
    if (this.spec != null) {
      this.spec.setIdentifier(identifier);
    }
  }

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ManifestConfig(String uuid, String identifier, ManifestConfigType type, ManifestAttributes spec) {
    this.uuid = uuid;
    this.identifier = identifier;
    this.type = type;
    this.spec = spec;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.SPEC, spec);
    return children;
  }
}
