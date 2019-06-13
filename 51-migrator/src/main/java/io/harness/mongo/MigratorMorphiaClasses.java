package io.harness.mongo;

import com.google.common.collect.ImmutableSet;

import io.harness.beans.database.MigrationJobInstance;

import java.util.Set;

public class MigratorMorphiaClasses {
  public static final Set<Class> classes = ImmutableSet.<Class>of(MigrationJobInstance.class);
}
