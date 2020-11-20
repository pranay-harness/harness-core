package io.harness.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface Expression {
  String ALLOW_SECRETS = "ALLOW_SECRETS";
  String DISALLOW_SECRETS = "DISALLOW_SECRETS";

  public enum SecretsMode { ALLOW_SECRETS, DISALLOW_SECRETS }

  String value();
}
