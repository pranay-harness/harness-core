package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.beans.SampleBean;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.ng.core.EntityDetail;

import com.google.inject.Singleton;
import java.util.function.Supplier;

@Singleton
public class SampleBeanEntityGitPersistenceHelperServiceImpl
    implements EntityGitPersistenceHelperService<SampleBean, SampleBean> {
  @Override
  public EntityType getEntityType() {
    return EntityType.CONNECTORS;
  }

  @Override
  public Supplier<SampleBean> getEntityFromYaml(SampleBean yaml) {
    return null;
  }

  @Override
  public EntityDetail getEntityDetail(SampleBean entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .identifier(entity.getIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .build())
        .build();
  }
}
