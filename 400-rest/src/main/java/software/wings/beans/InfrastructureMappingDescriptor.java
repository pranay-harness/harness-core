package software.wings.beans;

import software.wings.stencils.Stencil;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by peeyushaggarwal on 4/11/17.
 */
public interface InfrastructureMappingDescriptor extends Stencil<InfrastructureMapping> {
  @Override @JsonIgnore Class<? extends InfrastructureMapping> getTypeClass();
}
