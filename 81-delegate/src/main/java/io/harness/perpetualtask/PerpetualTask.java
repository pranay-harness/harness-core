package io.harness.perpetualtask;

import java.util.concurrent.Callable;

public interface PerpetualTask extends Callable<Void> { void stop(); }
