package io.harness.testframework.framework.matchers;

public interface Matcher<T> { boolean matches(T expected, T actual); }
