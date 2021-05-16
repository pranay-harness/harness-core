package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.EntityWithCount;
import io.harness.gitsync.common.beans.EntityWithCount.EntityWithCountKeys;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.repository.support.PageableExecutionUtils;

@UtilityClass
@OwnedBy(DX)
public class EntityDistinctElementHelper {
  public <T> Page<T> getDistinctElementPage(
      MongoTemplate mongoTemplate, Criteria criteria, Pageable pageable, String distinctKey, Class<T> entityClass) {
    final MatchOperation matchStage = Aggregation.match(criteria);
    final GroupOperation groupOperation =
        Aggregation.group(distinctKey).first(Aggregation.ROOT).as(EntityWithCountKeys.object);
    final GroupOperation distinctGroupStage = Aggregation.group(distinctKey);
    final CountOperation totalStage = Aggregation.count().as(EntityWithCountKeys.total);

    final Aggregation aggregationForEntity =
        Aggregation.newAggregation(Aggregation.skip(pageable.getPageNumber() * pageable.getPageSize()),
            Aggregation.limit(pageable.getPageSize()), matchStage, groupOperation);
    final Aggregation aggregationForCount = Aggregation.newAggregation(matchStage, distinctGroupStage, totalStage);
    final String collectionName = mongoTemplate.getCollectionName(entityClass);

    final List<EntityWithCount> mappedResults =
        mongoTemplate.aggregate(aggregationForEntity, collectionName, EntityWithCount.class).getMappedResults();

    return (Page<T>) PageableExecutionUtils.getPage(
        mappedResults.stream().map(EntityWithCount::getObject).collect(Collectors.toList()), pageable,
        ()
            -> mongoTemplate.aggregate(aggregationForCount, collectionName, EntityWithCount.class)
                   .getMappedResults()
                   .get(0)
                   .getTotal());
  }
}
