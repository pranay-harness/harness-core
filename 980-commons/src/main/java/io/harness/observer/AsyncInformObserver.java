package io.harness.observer;

import java.util.concurrent.ExecutorService;

public interface AsyncInformObserver extends Observer { ExecutorService getInformExecutorService(); }
