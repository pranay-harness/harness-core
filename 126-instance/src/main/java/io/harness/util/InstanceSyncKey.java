/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.util;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.String.valueOf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
@OwnedBy(HarnessTeam.CDP)
public class InstanceSyncKey {
  private static final String DEFAULT_DELIMITER = "_";
  private static final String KEY_PATTERN = "%s%s%s";
  Class<?> clazz;
  List<String> parts;

  private InstanceSyncKey(Class<?> clazz, List<String> parts) {
    this.clazz = clazz;
    this.parts = parts;
  }

  public static InstanceSyncKeyBuilder builder() {
    return new InstanceSyncKeyBuilder();
  }

  public String toString() {
    if (clazz == null) {
      return join(DEFAULT_DELIMITER, parts);
    }
    return format(KEY_PATTERN, clazz.getSimpleName(), DEFAULT_DELIMITER, join(DEFAULT_DELIMITER, parts));
  }

  public static class InstanceSyncKeyBuilder {
    private Class<?> clazz;
    private final List<String> parts = new LinkedList<>();

    InstanceSyncKeyBuilder() {}

    public InstanceSyncKeyBuilder clazz(Class<?> clazz) {
      this.clazz = clazz;
      return this;
    }

    public InstanceSyncKeyBuilder part(Object part) {
      if (Objects.isNull(part)) {
        throw new InvalidArgumentsException("Instance sync key part cannot be null");
      }

      String strValueOfPart = valueOf(part);
      if (StringUtils.isBlank(strValueOfPart)) {
        throw new InvalidArgumentsException("Instance sync key part cannot be empty");
      }

      parts.add(strValueOfPart);
      return this;
    }

    public InstanceSyncKey build() {
      return new InstanceSyncKey(clazz, parts);
    }
  }
}
