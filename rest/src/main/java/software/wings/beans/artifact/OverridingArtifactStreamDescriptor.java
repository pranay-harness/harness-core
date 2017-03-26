package software.wings.beans.artifact;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import java.util.Optional;

/**
 * Created by anubhaw on 1/25/17.
 */
public class OverridingArtifactStreamDescriptor
    implements OverridingStencil<ArtifactStream>, ArtifactStreamTypeDescriptor {
  private ArtifactStreamTypeDescriptor artifactStreamTypeDescriptor;
  private Optional<String> overridingName = Optional.empty();
  private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  public OverridingArtifactStreamDescriptor(ArtifactStreamTypeDescriptor artifactStreamTypeDescriptor) {
    this.artifactStreamTypeDescriptor = artifactStreamTypeDescriptor;
  }

  @Override
  public String getType() {
    return artifactStreamTypeDescriptor.getType();
  }

  @Override
  @JsonIgnore
  public Class<? extends ArtifactStream> getTypeClass() {
    return artifactStreamTypeDescriptor.getTypeClass();
  }

  @Override
  public JsonNode getJsonSchema() {
    return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy()
                                            : artifactStreamTypeDescriptor.getJsonSchema();
  }

  @Override
  public Object getUiSchema() {
    return artifactStreamTypeDescriptor.getUiSchema();
  }

  @Override
  public String getName() {
    return overridingName.orElse(artifactStreamTypeDescriptor.getName());
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return artifactStreamTypeDescriptor.getOverridingStencil();
  }

  @Override
  public ArtifactStream newInstance(String id) {
    return artifactStreamTypeDescriptor.newInstance(id);
  }

  @Override
  public boolean matches(Object context) {
    return artifactStreamTypeDescriptor.matches(context);
  }

  @Override
  public JsonNode getOverridingJsonSchema() {
    return overridingJsonSchema.orElse(null);
  }

  @Override
  public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
    this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
  }

  @Override
  public String getOverridingName() {
    return overridingName.orElse(null);
  }

  @Override
  public void setOverridingName(String overridingName) {
    this.overridingName = Optional.ofNullable(overridingName);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return artifactStreamTypeDescriptor == null ? null : artifactStreamTypeDescriptor.getStencilCategory();
  }

  @Override
  public Integer getDisplayOrder() {
    return artifactStreamTypeDescriptor == null ? DEFAULT_DISPLAY_ORDER
                                                : artifactStreamTypeDescriptor.getDisplayOrder();
  }
}
