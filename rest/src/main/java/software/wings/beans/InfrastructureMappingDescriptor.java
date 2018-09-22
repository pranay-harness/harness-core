package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.stencils.Stencil;

/**
 * Created by peeyushaggarwal on 4/11/17.
 */
public interface InfrastructureMappingDescriptor extends Stencil<InfrastructureMapping> {
  @JsonIgnore Class<? extends InfrastructureMapping> getTypeClass();
}
