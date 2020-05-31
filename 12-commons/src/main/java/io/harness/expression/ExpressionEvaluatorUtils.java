package io.harness.expression;

import static java.lang.String.format;

import io.harness.data.algorithm.IdentifierName;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.FunctorException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrSubstitutor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
@Slf4j
public class ExpressionEvaluatorUtils {
  public final int EXPANSION_LIMIT = 256 * 1024; // 256 KB
  private final int EXPANSION_MULTIPLIER_LIMIT = 10;
  private final int DEPTH_LIMIT = 10;

  private final JexlEngine engine = new JexlBuilder().logger(new NoOpLog()).create();

  /**
   * Fetch field from inside the object using jexl conventions. POJSs, Maps, Classes having get method are supported.
   *
   * @param obj the object whose field we have to find
   * @param field the field name
   * @return value wrapped in optional if field is found, else Optional.none()
   */
  public Optional<Object> fetchField(Object obj, String field) {
    if (obj == null || field == null) {
      return Optional.empty();
    }

    try {
      Object retObj = engine.getProperty(obj, field);
      return Optional.of(retObj);
    } catch (JexlException ex) {
      if (ex.getCause() instanceof FunctorException) {
        throw(FunctorException) ex.getCause();
      }

      logger.debug(format("Could not fetch field '%s'", field), ex);
      return Optional.empty();
    }
  }

  public String substitute(
      ExpressionEvaluatorItfc expressionEvaluator, String expression, JexlContext jc, VariableResolverTracker tracker) {
    if (expression == null) {
      return null;
    }

    String prefix = IdentifierName.random();
    String suffix = IdentifierName.random();

    Pattern pattern = Pattern.compile(prefix + "[0-9]+" + suffix);

    final EvaluateVariableResolver variableResolver = EvaluateVariableResolver.builder()
                                                          .expressionEvaluator(expressionEvaluator)
                                                          .context(jc)
                                                          .variableResolverTracker(tracker)
                                                          .prefix(prefix)
                                                          .suffix(suffix)
                                                          .build();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setEnableSubstitutionInVariables(true);
    substitutor.setVariableResolver(variableResolver);

    String result = expression;
    int limit = Math.max(EXPANSION_LIMIT, EXPANSION_MULTIPLIER_LIMIT * expression.length());
    for (int i = 0; i < DEPTH_LIMIT; i++) {
      String original = result;
      result = substitutor.replace(new StringBuffer(original));

      for (;;) {
        final Matcher matcher = pattern.matcher(result);
        if (!matcher.find()) {
          break;
        }

        StringBuffer sb = new StringBuffer();
        do {
          String name = matcher.group(0);
          String value = String.valueOf(jc.get(name));
          matcher.appendReplacement(sb, value.replace("\\", "\\\\").replace("$", "\\$"));
        } while (matcher.find());
        matcher.appendTail(sb);
        result = sb.toString();
      }
      if (result.equals(original)) {
        return result;
      }
      if (result.length() > limit) {
        throw new CriticalExpressionEvaluationException("Exponentially growing interpretation", expression);
      }
    }

    throw new CriticalExpressionEvaluationException(
        "Infinite loop or too deep indirection in property interpretation", expression);
  }
}
