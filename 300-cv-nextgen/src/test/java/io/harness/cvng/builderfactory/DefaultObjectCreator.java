package io.harness.cvng.builderfactory;

import io.harness.cvng.BuilderFactory;

public interface DefaultObjectCreator<T> {
  T create(BuilderFactory builderFactory);
}
