package io.harness.resolvers;

import io.harness.refrences.RefObject;
import io.harness.state.io.StateTransput;

public interface Resolver { StateTransput resolve(RefObject refObject); }
