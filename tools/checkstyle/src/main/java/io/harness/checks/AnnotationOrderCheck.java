/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks;

import io.harness.checks.mixin.AnnotationMixin;
import io.harness.checks.mixin.ModifierMixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationOrderCheck extends AbstractCheck {
  private static final String ORDER_MSG_KEY = "code.readability.annotation.order";
  private static final String MODIFIER_MSG_KEY = "code.readability.annotation.modifier";
  private static final String REQUIRED_MSG_KEY = "code.readability.annotation.required.missing";
  private static final String INCOMPATIBLE_MSG_KEY = "annotation.problem.incompatible";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.ANNOTATION,
    };
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[] {
        TokenTypes.ANNOTATION,
    };
  }

  Map<String, Integer> annotationOrder = ImmutableMap.<String, Integer>builder()
                                             .put("Value", 1)
                                             .put("Data", 1)
                                             .put("Getter", 2)
                                             .put("Setter", 3)
                                             .put("Builder", 11)
                                             .put("NoArgsConstructor", 12)
                                             .put("AllArgsConstructor", 13)
                                             .put("ToString", 21)
                                             .put("EqualsAndHashCode", 22)
                                             .put("Test", 31)
                                             .put("Owner", 32)
                                             .put("Repeat", 33)
                                             .put("Category", 34)
                                             .put("Ignore", 35)
                                             .put("JsonIgnore", 41)
                                             .put("SchemaIgnore", 51)
                                             .put("Entity", 61)
                                             .put("HarnessEntity", 62)
                                             .put("UtilityClass", 101)
                                             .put("Slf4j", 102)
                                             .build();

  Map<String, Map<String, String>> incompatible =
      ImmutableMap.<String, Map<String, String>>builder()
          .put("SchemaIgnore",
              ImmutableMap.<String, String>builder()
                  .put("Getter",
                      "Lombok is not propagating the field annotations when creating getter methods. "
                          + "This will result of @SchemaIgnore annotation to be noop. You cannot use them on the same field.")
                  .build())
          .build();

  Map<String, Set<String>> required = ImmutableMap.<String, Set<String>>builder()
                                          .put("Ignore", ImmutableSet.<String>builder().add("Owner").build())
                                          .put("HarnessEntity", ImmutableSet.<String>builder().add("Entity").build())
                                          .build();

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST annotation) {
    final String name = AnnotationMixin.name(annotation);
    final Integer order = annotationOrder.get(name);

    Map<String, String> incompatibleMap = incompatible.get(name);
    Set<String> requiredSet = new HashSet<>(required.getOrDefault(name, new HashSet<>()));

    DetailAST prevAnnotation = annotation.getPreviousSibling();
    if (order != null) {
      while (prevAnnotation != null && prevAnnotation.getType() == TokenTypes.ANNOTATION) {
        final String prevName = AnnotationMixin.name(prevAnnotation);
        requiredSet.remove(prevName);

        if (incompatibleMap != null) {
          String msg = incompatibleMap.get(prevName);
          if (msg != null) {
            log(annotation, INCOMPATIBLE_MSG_KEY, name, prevName, msg);
          }
        }

        prevAnnotation = prevAnnotation.getPreviousSibling();

        final Integer prevOrder = annotationOrder.get(prevName);
        if (prevOrder == null) {
          continue;
        }

        if (prevOrder > order) {
          log(annotation, ORDER_MSG_KEY, name, prevName);
        }
      }
    }

    if (!requiredSet.isEmpty()) {
      log(annotation, REQUIRED_MSG_KEY, name, String.join(", ", requiredSet));
    }

    if (prevAnnotation != null && ModifierMixin.isModifier(prevAnnotation)) {
      log(annotation, MODIFIER_MSG_KEY, name);
    }
  }
}
