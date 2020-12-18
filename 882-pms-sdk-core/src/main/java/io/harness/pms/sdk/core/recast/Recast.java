package io.harness.pms.sdk.core.recast;

import java.util.Set;

/**
 * The translation layer for conversion from PipelineService json string to sdk objects
 */
public class Recast {
  private final Recaster recaster;

  public Recast(final Set<Class> classesToMap) {
    this(new Recaster(), classesToMap);
  }

  public Recast(final Recaster recaster, final Set<Class> classesToMap) {
    this.recaster = recaster;
    for (final Class c : classesToMap) {
      map(c);
    }
  }

  public synchronized Recast map(final Class... entityClasses) {
    if (entityClasses != null && entityClasses.length > 0) {
      for (final Class entityClass : entityClasses) {
        if (!recaster.isCasted(entityClass)) {
          recaster.addCastedClass(entityClass);
        }
      }
    }
    return this;
  }
}
