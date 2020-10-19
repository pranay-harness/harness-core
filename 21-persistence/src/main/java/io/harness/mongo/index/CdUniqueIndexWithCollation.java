package io.harness.mongo.index;

import static io.harness.mongo.CollationLocale.ENGLISH;
import static io.harness.mongo.CollationStrength.PRIMARY;

import io.harness.mongo.CollationLocale;
import io.harness.mongo.CollationStrength;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CdUniqueIndexesWithCollation.class)
@Deprecated
public @interface CdUniqueIndexWithCollation {
  String name();
  Field[] fields();
  CollationLocale locale() default ENGLISH;
  CollationStrength strength() default PRIMARY;
}
