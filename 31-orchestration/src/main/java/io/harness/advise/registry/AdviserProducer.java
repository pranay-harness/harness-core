package io.harness.advise.registry;

import io.harness.advise.Adviser;
import io.harness.advise.AdviserParameters;

public interface AdviserProducer { Adviser produce(AdviserParameters adviserParameters); }
