package io.harness.app;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import io.harness.exception.UnsupportedOperationException;
import io.harness.mongo.index.Field;
import io.harness.ng.RsqlQueryable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
