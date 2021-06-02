package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeEvent;

/**
 * The changeHandler interface each search entity
 * would implement.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
public interface ChangeHandler {
  boolean handleChange(ChangeEvent<?> changeEvent);
}
