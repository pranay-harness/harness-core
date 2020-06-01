package io.harness.expression;

import static java.lang.String.format;

import io.harness.data.algorithm.IdentifierName;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.utils.ParameterField;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrSubstitutor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
@Slf4j
public class ExpressionEvaluatorUtils {
  public final int EXPANSION_LIMIT = 256 * 1024; // 256 KB
  public final int EXPANSION_MULTIPLIER_LIMIT = 10;
  public final int DEPTH_LIMIT = 10;

  private final JexlEngine engine = new JexlBuilder().logger(new NoOpLog()).create();

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
      logger.debug(format("Could not fetch field '%s'", field), ex);
      return Optional.empty();
    }
  }

  public interface ResolveFunctor {
    String renderExpression(String str);
    Optional<Object> evaluateExpressionOptional(String str);
  }

  /**
   * Update expression values inside object recursively. The new value is obtained using the functor. If the field has a
   * NotExpression annotation, it is skipped. String fields are rendered and ParameterFields are evaluated if they have
   * an expression.
   *
   * @param o             the object to update
   * @param functor       the functor which provides the evaluated and rendered values
   * @return the new object with updated objects (this can be done in-place or a new object can be returned)
   */
  public Object updateExpressions(Object o, ResolveFunctor functor) {
    Object updatedObj = updateExpressionsInternal(o, functor, new HashSet<>());
    return updatedObj == null ? o : updatedObj;
  }

  private Object updateExpressionsInternal(Object obj, ResolveFunctor functor, Set<Integer> cache) {
    if (obj == null) {
      return null;
    }

    Class<?> c = obj.getClass();
    if (ClassUtils.isPrimitiveOrWrapper(c)) {
      return null;
    }

    if (obj instanceof String) {
      return functor.renderExpression((String) obj);
    }

    // In case of array, update in-place and return null.
    if (c.isArray()) {
      if (c.getComponentType().isPrimitive()) {
        return null;
      }

      int length = Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        Object arrObj = Array.get(obj, i);
        Object newArrObj = updateExpressionsInternal(arrObj, functor, cache);
        if (newArrObj != null) {
          Array.set(obj, i, newArrObj);
        }
      }

      return null;
    }

    // In case of object, iterate over fields and update them in a similar manner.
    boolean updated = updateExpressionFields(obj, functor, cache);
    if (!updated) {
      return null;
    }

    return obj;
  }

  private boolean updateExpressionFields(Object obj, ResolveFunctor functor, Set<Integer> cache) {
    if (obj == null) {
      return false;
    }

    int hashCode = System.identityHashCode(obj);
    if (cache.contains(hashCode)) {
      return false;
    } else {
      cache.add(hashCode);
    }

    if (obj instanceof ParameterField) {
      ParameterField<?> parameterField = (ParameterField<?>) obj;
      Object value;
      if (parameterField.isExpression()) {
        Optional<Object> optional = functor.evaluateExpressionOptional(parameterField.getExpressionValue());
        if (optional.isPresent()) {
          value = optional.get();
        } else {
          return false;
        }
      } else {
        value = parameterField.getValue();
      }

      if (value != null) {
        Object newValue = updateExpressionsInternal(value, functor, cache);
        if (newValue == null) {
          newValue = value;
        }
        parameterField.updateWithValue(newValue);
      } else {
        parameterField.updateWithValue(null);
      }
      return true;
    }

    Class<?> c = obj.getClass();
    boolean updated = false;
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        // Ignore field if skipPredicate returns true or if the field is static.
        if (f.isAnnotationPresent(NotExpression.class) || Modifier.isStatic(f.getModifiers())) {
          continue;
        }

        boolean isAccessible = f.isAccessible();
        f.setAccessible(true);
        try {
          if (updateExpressionFieldsInternal(obj, f, functor, cache)) {
            updated = true;
          }
          f.setAccessible(isAccessible);
        } catch (IllegalAccessException ignored) {
          logger.error("Field [{}] is not accessible", f.getName());
        }
      }
      c = c.getSuperclass();
    }

    return updated;
  }

  private boolean updateExpressionFieldsInternal(Object o, Field f, ResolveFunctor functor, Set<Integer> cache)
      throws IllegalAccessException {
    if (f == null) {
      return false;
    }

    Object obj = f.get(o);
    Object updatedObj = updateExpressionsInternal(obj, functor, cache);
    if (updatedObj != null) {
      f.set(o, updatedObj);
      return true;
    }

    return false;
  }
}
