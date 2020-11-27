package io.harness.walktree.registries.visitorfield;

/**
 * This interface should be implemented by that Visitable which has custom java class as their leaf property.
 */
public interface VisitorFieldWrapper {
  VisitorFieldType getVisitorFieldType();
}
