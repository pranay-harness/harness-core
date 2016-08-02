package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.stencils.Stencil;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface CommandUnitDescriptor extends ExtensionPoint, Stencil<CommandUnit> {
  @JsonIgnore Class<? extends CommandUnit> getTypeClass();
}
