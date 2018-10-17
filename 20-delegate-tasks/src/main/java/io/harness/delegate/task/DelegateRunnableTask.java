package io.harness.delegate.task;

import io.harness.delegate.task.protocol.ResponseData;

public interface DelegateRunnableTask extends Runnable { ResponseData run(Object[] parameters); }
