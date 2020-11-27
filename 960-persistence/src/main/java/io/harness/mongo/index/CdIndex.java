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
public @interface CdIndex {
  String name();
  Field[] fields();
}
