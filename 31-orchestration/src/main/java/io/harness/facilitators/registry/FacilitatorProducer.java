package io.harness.facilitators.registry;

import io.harness.facilitate.Facilitator;
import io.harness.facilitate.io.FacilitatorParameters;

public interface FacilitatorProducer { Facilitator produce(FacilitatorParameters parameters); }
