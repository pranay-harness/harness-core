package io.harness.repositories;

import io.harness.event.QueryRecordEntity;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class QueryRecordsRepositoryCustomImpl implements QueryRecordsRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Map<String, Set<String>> findAllHashes(int page, int size) {
    Map<String, Set<String>> serviceNameToHashes = new HashMap<>();
    Pageable pageable = PageUtils.getPageRequest(page, size, new ArrayList<>());
    Query query = new Query().with(pageable);
    mongoTemplate.find(query, QueryRecordEntity.class).forEach(queryRecordEntity -> {
      Set<String> storedHashes = serviceNameToHashes.getOrDefault(queryRecordEntity.getServiceName(), new HashSet<>());
      storedHashes.add(queryRecordEntity.getHash());
      serviceNameToHashes.put(queryRecordEntity.getServiceName(), storedHashes);
    });
    return serviceNameToHashes;
  }
}
