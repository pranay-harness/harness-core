package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNRESOLVED_EXPRESSIONS_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.Level;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PIPELINE)
public class UnresolvedExpressionsException extends WingsException {
  private static final String NULL_STR = "null";

  public static final String EXPRESSIONS_ARG = "expressions";

  public UnresolvedExpressionsException(List<String> expressions) {
    super(null, null, UNRESOLVED_EXPRESSIONS_ERROR, Level.ERROR, null, null);
    super.param(EXPRESSIONS_ARG,
        expressions == null ? NULL_STR
                            : expressions.stream().filter(Objects::nonNull).collect(Collectors.joining(", ")));
  }

  public Collection<String> fetchExpressions() {
    String expressionsParam = ((String) getParams().get(EXPRESSIONS_ARG)).trim();
    if (EmptyPredicate.isEmpty(expressionsParam) || expressionsParam.equals(NULL_STR)) {
      return Collections.emptyList();
    }
    return Arrays.stream(StringUtils.split(expressionsParam, ","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }
}
