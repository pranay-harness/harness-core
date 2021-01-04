package io.harness.annotations.retry;

@FunctionalInterface
public interface IMethodWrapper<T> {
  T execute() throws Throwable;
}
