package io.harness.ng.core;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import io.harness.mongo.index.Field;
import io.harness.ng.RsqlQueryable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * This class still needs improvement.
 * For now, keeping it this way to that UI team can start work.
 * Will re-visit it.s
 */
@Singleton
public class RestQueryFilterParser {
  private final QueryConversionPipeline pipeline;

  public RestQueryFilterParser() {
    pipeline = QueryConversionPipeline.defaultPipeline();
  }

  public Criteria getCriteriaFromFilterQuery(@NotNull String filterQuery, Class<?> targetEntityClass) {
    Criteria criteria = new Criteria();

    if (isNotBlank(filterQuery)) {
      Field[] fields = targetEntityClass.getAnnotation(RsqlQueryable.class).fields();
      Set<String> set = new HashSet<>();
      for (Field field : fields) {
        set.add(field.value());
      }
      ConstrainedMongoVisitor constrainedMongoVisitor = new ConstrainedMongoVisitor(set);
      Condition<GeneralQueryBuilder> condition = pipeline.apply(filterQuery, targetEntityClass);
      criteria = condition.query(constrainedMongoVisitor);
    }
    return criteria;
  }
}
