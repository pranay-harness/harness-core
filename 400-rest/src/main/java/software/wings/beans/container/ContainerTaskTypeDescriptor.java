package software.wings.beans.container;

import software.wings.stencils.Stencil;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by anubhaw on 2/6/17.
 */
public interface ContainerTaskTypeDescriptor extends Stencil<ContainerTask> {
  @Override @JsonIgnore Class<? extends ContainerTask> getTypeClass();
}
