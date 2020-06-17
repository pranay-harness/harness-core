package io.harness.ng.core;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.validation.constraints.NotNull;

/**
 * This class still needs improvement.
 * For now, keeping it this way to that UI team can start work.
 * Will re-visit it.s
 */
@Singleton
public class RestQueryFilterParser {
  private final QueryConversionPipeline pipeline;
  private final MongoVisitor mongoVisitor;

  public RestQueryFilterParser() {
    pipeline = QueryConversionPipeline.defaultPipeline();
    mongoVisitor = new MongoVisitor();
  }

  public Criteria getCriteriaFromFilterQuery(@NotNull String filterQuery, Class<?> targetEntityClass) {
    Criteria criteria = new Criteria();
    if (isNotBlank(filterQuery)) {
      Condition<GeneralQueryBuilder> condition = pipeline.apply(filterQuery, targetEntityClass);
      criteria = condition.query(mongoVisitor);
    }
    return criteria;
  }
}
