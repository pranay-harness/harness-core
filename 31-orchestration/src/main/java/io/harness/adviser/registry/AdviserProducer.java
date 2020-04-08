package io.harness.adviser.registry;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserParameters;

public interface AdviserProducer { Adviser produce(AdviserParameters adviserParameters); }
