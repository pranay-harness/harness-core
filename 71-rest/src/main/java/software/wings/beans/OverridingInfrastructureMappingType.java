package software.wings.beans;

/**
 * Created by peeyushaggarwal on 4/11/17.
 */
import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/28/16.
 */
public class OverridingInfrastructureMappingType
    implements OverridingStencil<InfrastructureMapping>, InfrastructureMappingDescriptor {
  private InfrastructureMappingDescriptor infrastructureMappingDescriptor;
  private Optional<String> overridingName = Optional.empty();
  private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  public OverridingInfrastructureMappingType(InfrastructureMappingDescriptor infrastructureMappingDescriptor) {
    this.infrastructureMappingDescriptor = infrastructureMappingDescriptor;
  }

  @Override
  public String getType() {
    return infrastructureMappingDescriptor.getType();
  }

  @Override
  @JsonIgnore
  public Class<? extends InfrastructureMapping> getTypeClass() {
    return infrastructureMappingDescriptor.getTypeClass();
  }

  @Override
  public JsonNode getJsonSchema() {
    return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy()
                                            : infrastructureMappingDescriptor.getJsonSchema();
  }

  @Override
  public Object getUiSchema() {
    return infrastructureMappingDescriptor.getUiSchema();
  }

  @Override
  public String getName() {
    return overridingName.orElse(infrastructureMappingDescriptor.getName());
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return infrastructureMappingDescriptor.getOverridingStencil();
  }

  @Override
  public InfrastructureMapping newInstance(String id) {
    return infrastructureMappingDescriptor.newInstance(id);
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
    return infrastructureMappingDescriptor == null ? null : infrastructureMappingDescriptor.getStencilCategory();
  }

  @Override
  public Integer getDisplayOrder() {
    return infrastructureMappingDescriptor == null ? Integer.valueOf(DEFAULT_DISPLAY_ORDER)
                                                   : infrastructureMappingDescriptor.getDisplayOrder();
  }

  @Override
  public boolean matches(Object context) {
    return infrastructureMappingDescriptor.matches(context);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", getType())
        .add("typeClass", getTypeClass())
        .add("jsonSchema", getJsonSchema())
        .add("uiSchema", getUiSchema())
        .add("name", getName())
        .add("stencilCategory", getStencilCategory())
        .add("displayOrder", getDisplayOrder())
        .toString();
  }
}
