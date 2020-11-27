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
/**
 * create a static method named mongoIndexes instead.
 * Ex:
 * public static List<MongoIndex> mongoIndexes() {
 *     return ImmutableList.<MongoIndex>builder()
 *         .add(CompoundMongoIndex.builder()
 *                  .name("iterator")
 *                  .unique(false)
 *                  .field(CVNGVerificationTaskKeys.status)
 *                  .field(CVNGVerificationTaskKeys.cvngVerificationTaskIteration)
 *                  .build())
 *         .build();
 *   }
 */
public @interface CdUniqueIndexWithCollation {
  String name();
  Field[] fields();
  CollationLocale locale() default ENGLISH;
  CollationStrength strength() default PRIMARY;
}
