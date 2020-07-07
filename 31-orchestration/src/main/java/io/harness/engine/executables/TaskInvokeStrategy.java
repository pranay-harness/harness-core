package io.harness.engine.executables;

import io.harness.tasks.TaskMode;

public interface TaskInvokeStrategy extends InvokeStrategy { TaskMode getMode(); }
