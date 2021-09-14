/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.mapper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TagMapper {
  public static List<NGTag> convertToList(Map<String, String> tags) {
    if (isEmpty(tags)) {
      return EMPTY_LIST;
    }

    return tags.entrySet()
        .stream()
        .map(e -> NGTag.builder().key(e.getKey()).value(e.getValue()).build())
        .collect(toList());
  }

  public static Map<String, String> convertToMap(List<NGTag> tags) {
    if (isEmpty(tags)) {
      return EMPTY_MAP;
    }

    return tags.stream().collect(toMap(NGTag::getKey, NGTag::getValue));
  }
}
