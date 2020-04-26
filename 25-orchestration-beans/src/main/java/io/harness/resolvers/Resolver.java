package io.harness.resolvers;

import io.harness.annotations.Redesign;
import io.harness.references.RefObject;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;

@Redesign
public interface Resolver {
  StateTransput resolve(Ambiance ambiance, RefObject refObject);
}
