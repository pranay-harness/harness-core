package software.wings.usage.scope;

import io.harness.beans.UsageRestrictions;

/**
 * Interface to represent usage scope of entities
 */
public interface ScopedEntity {
  boolean isScopedToAccount();
  UsageRestrictions getUsageRestrictions();
}
