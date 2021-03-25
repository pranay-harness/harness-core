package io.harness.ng.core.mapper;

import static io.harness.data.structure.HasPredicate.hasNone;

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
    if (hasNone(tags)) {
      return EMPTY_LIST;
    }

    return tags.entrySet()
        .stream()
        .map(e -> NGTag.builder().key(e.getKey()).value(e.getValue()).build())
        .collect(toList());
  }

  public static Map<String, String> convertToMap(List<NGTag> tags) {
    if (hasNone(tags)) {
      return EMPTY_MAP;
    }

    return tags.stream().collect(toMap(NGTag::getKey, NGTag::getValue));
  }
}
