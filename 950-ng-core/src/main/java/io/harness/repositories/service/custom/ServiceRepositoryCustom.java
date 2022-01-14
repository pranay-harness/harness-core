package io.harness.repositories.service.custom;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.service.entity.ServiceEntity;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ServiceRepositoryCustom {
  Page<ServiceEntity> findAll(Criteria criteria, Pageable pageable);
  ServiceEntity upsert(Criteria criteria, ServiceEntity serviceEntity);
  ServiceEntity update(Criteria criteria, ServiceEntity serviceEntity);
  UpdateResult delete(Criteria criteria);
  Long findActiveServiceCountAtGivenTimestamp(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);

  List<ServiceEntity> findAllRunTimePermission(Criteria criteria);

  ServiceEntity find(String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      boolean deleted);
}
