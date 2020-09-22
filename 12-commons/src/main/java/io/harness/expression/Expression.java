package io.harness.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface Expression {
  public final static String ALLOW_SECRETS = "ALLOW_SECRETS";
  public final static String DISALLOW_SECRETS = "DISALLOW_SECRETS";

  enum SecretsMode { ALLOW_SECRETS, DISALLOW_SECRETS }

  String value();
}