package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.stencils.Stencil;

import java.util.List;

/**
 * Plugin interface for adding state.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
