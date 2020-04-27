package io.harness.resolvers;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.references.RefObject;
import io.harness.state.io.StateTransput;

@Redesign
public interface Resolver {
  StateTransput resolve(Ambiance ambiance, RefObject refObject);
}
