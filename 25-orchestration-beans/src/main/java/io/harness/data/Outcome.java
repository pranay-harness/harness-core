package io.harness.data;

import io.harness.state.io.StateTransput;

public interface Outcome extends StateTransput { OutcomeType getOutcomeType(); }
