package io.harness.engine.advise;

import io.harness.adviser.Advise;
import io.harness.state.io.ambiance.Ambiance;

public interface AdviseHandler { void handleAdvise(Ambiance ambiance, Advise advise); }
