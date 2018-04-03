package io.harness.distribution.barrier;

import java.util.Map;

public interface ForceProctor { Forcer.State getForcerState(ForcerId forcerId, Map<String, Object> metadata); }
