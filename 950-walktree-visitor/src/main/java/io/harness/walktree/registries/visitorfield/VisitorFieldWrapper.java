/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.registries.visitorfield;

/**
 * This interface should be implemented by that Visitable which has custom java class as their leaf property.
 */
public interface VisitorFieldWrapper {
  VisitorFieldType getVisitorFieldType();
}
