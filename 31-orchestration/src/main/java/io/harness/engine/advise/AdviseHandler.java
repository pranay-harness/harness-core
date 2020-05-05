package io.harness.engine.advise;

import io.harness.adviser.Advise;
import io.harness.ambiance.Ambiance;

public interface AdviseHandler<T extends Advise> { void handleAdvise(Ambiance ambiance, T advise); }
