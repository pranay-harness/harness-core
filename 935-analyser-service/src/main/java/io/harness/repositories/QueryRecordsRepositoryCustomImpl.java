package io.harness.repositories;

import io.harness.event.QueryRecordEntity;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class QueryRecordsRepositoryCustomImpl implements QueryRecordsRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<QueryRecordEntity> findAllHashes(int page, int size) {
    Pageable pageable = PageUtils.getPageRequest(page, size, new ArrayList<>());
    Query query = new Query().with(pageable);
    return mongoTemplate.find(query, QueryRecordEntity.class);
  }
}
