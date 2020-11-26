package io.harness.mongo.index;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CdIndexes.class)
@Deprecated
public @interface CdIndex {
  String name();
  Field[] fields();
}
