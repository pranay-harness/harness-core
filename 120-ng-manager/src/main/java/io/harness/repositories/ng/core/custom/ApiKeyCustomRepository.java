package io.harness.repositories.ng.core.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.entities.ApiKey;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface ApiKeyCustomRepository {
  Page<ApiKey> findAll(Criteria criteria, Pageable pageable);

  <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn);

  Map<String, Integer> getApiKeysPerParentIdentifier(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, ApiKeyType apiKeyType, List<String> parentIdentifiers);
}
