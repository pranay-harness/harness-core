package io.harness.repositories.user.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserMetadata;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface UserMetadataRepositoryCustom {
  Page<UserMetadata> findAll(Criteria criteria, Pageable pageable);

  List<UserMetadata> findAll(Criteria criteria);

  long insertAllIgnoringDuplicates(List<UserMetadata> userMetadata);
}
