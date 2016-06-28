package software.wings.sm;

import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.stencils.Stencil;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Plugin interface for adding state.
 *
 * @author Rishi
 */
public interface StateTypeDescriptor extends ExtensionPoint, Stencil<State> {
  /**
   * Gets scopes.
   *
   * @return the scopes
   */
  List<StateTypeScope> getScopes();
}
