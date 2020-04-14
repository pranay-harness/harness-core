package io.harness.resolvers;

import io.harness.annotations.Redesign;
import io.harness.refrences.RefObject;
import io.harness.state.io.StateTransput;

@Redesign
public interface Resolver {
  StateTransput resolve(RefObject refObject);
}
