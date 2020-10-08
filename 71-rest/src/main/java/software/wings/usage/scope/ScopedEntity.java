package software.wings.usage.scope;

import software.wings.security.UsageRestrictions;

/**
 * Interface to represent usage scope of entities
 */
public interface ScopedEntity {
  boolean isScopedToAccount();
  UsageRestrictions getUsageRestrictions();
}
