package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.stencils.Stencil;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface CommandUnitDescriptor extends Stencil<CommandUnit> {
  @Override @JsonIgnore Class<? extends CommandUnit> getTypeClass();
}
