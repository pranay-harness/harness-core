package io.harness.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.visitor.helpers.manifest.ManifestConfigVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@SimpleVisitorHelper(helperClass = ManifestConfigVisitorHelper.class)
@TypeAlias("manifestConfig")
public class ManifestConfig implements Visitable {
  @EntityIdentifier String identifier;
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY)
  ManifestAttributes manifestAttributes;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setManifestAttributes(ManifestAttributes manifestAttributes) {
    this.manifestAttributes = manifestAttributes;
    if (this.manifestAttributes != null) {
      this.manifestAttributes.setIdentifier(identifier);
    }
  }

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ManifestConfig(String identifier, String type, ManifestAttributes manifestAttributes) {
    this.identifier = identifier;
    this.type = type;
    this.manifestAttributes = manifestAttributes;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.SPEC, manifestAttributes);
    return children;
  }
}
