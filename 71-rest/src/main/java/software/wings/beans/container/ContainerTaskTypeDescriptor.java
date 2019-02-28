package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.stencils.Stencil;

/**
 * Created by anubhaw on 2/6/17.
 */
public interface ContainerTaskTypeDescriptor extends Stencil<ContainerTask> {
  @JsonIgnore Class<? extends ContainerTask> getTypeClass();
}
