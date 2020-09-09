package io.harness.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.visitor.helpers.serviceconfig.ManifestConfigVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonTypeName("manifest")
@SimpleVisitorHelper(helperClass = ManifestConfigVisitorHelper.class)
public class ManifestConfig implements ManifestConfigWrapper, Visitable {
  String identifier;
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY)
  ManifestAttributes manifestAttributes;

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
  public List<Object> getChildrenToWalk() {
    List<Object> children = new ArrayList<>();
    children.add(manifestAttributes);
    return children;
  }
}