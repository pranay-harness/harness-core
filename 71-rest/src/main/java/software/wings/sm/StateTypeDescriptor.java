package software.wings.sm;

import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.stencils.Stencil;

import java.util.List;

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

  /**
   * Gets phase step types.
   *
   * @return the phase step types
   */
  List<String> getPhaseStepTypes();
}
