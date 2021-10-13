package io.harness.validation;

import static java.lang.annotation.ElementType.TYPE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
@OwnedBy(HarnessTeam.CDC)
public @interface OneOfSet {
  /**
   * Provide set of sets out of which one only set is required in schema validation.
   * If there are multiple fields in a single set, provide them as a single comma separated string.
   *
   * Eg, say we want either fields set {a, b, c} comes or fields set {e, f} comes, provide fields as:
   * fields = {"a, b, c", "e, f"}
   */
  String[] fields() default {};
}
