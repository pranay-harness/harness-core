/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.security;

/**
 * Interface to represent usage scope of entities
 */
public interface ScopedEntity {
  boolean isScopedToAccount();
  UsageRestrictions getUsageRestrictions();
}
