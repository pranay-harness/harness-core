package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.stencils.Stencil;

import java.util.List;

/**
 * Plugin interface for adding state.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
public interface StateTypeDescriptor extends Stencil<State> {
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
