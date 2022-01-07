/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.data.structure;

import static java.util.Collections.emptyMap;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Mongo doesn't allow dots in map keys. So define a replacement character that's not otherwise expected and map back &
 * forth to it.
 */
public class MongoMapSanitizer {
  private final char replacement;

  public MongoMapSanitizer(char replacement) {
    this.replacement = replacement;
  }

  public <T> Map<String, T> encodeDotsInKey(@Nullable Map<String, T> map) {
    return Optional.ofNullable(map)
        .orElse(emptyMap())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey().replace('.', replacement), Map.Entry::getValue));
  }

  public <T> Map<String, T> decodeDotsInKey(@Nullable Map<String, T> map) {
    return Optional.ofNullable(map)
        .orElse(emptyMap())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey().replace(replacement, '.'), Map.Entry::getValue));
  }
}
