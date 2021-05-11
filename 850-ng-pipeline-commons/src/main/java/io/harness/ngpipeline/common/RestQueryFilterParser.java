package io.harness.ngpipeline.common;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.exception.UnsupportedOperationException;
import io.harness.mongo.index.Field;
import io.harness.ng.RsqlQueryable;

import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * This class still needs improvement.
 * For now, keeping it this way to that UI team can start work.
 * Will re-visit it.s
 */
@Singleton
@Deprecated
@ToBeDeleted
public class RestQueryFilterParser {
  private final QueryConversionPipeline pipeline;

  public RestQueryFilterParser() {
    pipeline = QueryConversionPipeline.defaultPipeline();
  }

  public Criteria getCriteriaFromFilterQuery(@NotNull String filterQuery, Class<?> targetEntityClass) {
    Criteria criteria = new Criteria();

    if (isNotBlank(filterQuery)) {
      RsqlQueryable rsqlQueryable = targetEntityClass.getAnnotation(RsqlQueryable.class);
      if (rsqlQueryable == null) {
        throw new UnsupportedOperationException("Filter query is not supported");
      }
      List<Field> fields = Arrays.asList(rsqlQueryable.fields());
      Set<String> set = new HashSet<>();
      fields.forEach(field -> set.add(field.value()));
      ConstrainedMongoVisitor constrainedMongoVisitor = new ConstrainedMongoVisitor(set);
      Condition<GeneralQueryBuilder> condition = pipeline.apply(filterQuery, targetEntityClass);
      criteria = condition.query(constrainedMongoVisitor);
    }
    return criteria;
  }
}
