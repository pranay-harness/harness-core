/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
