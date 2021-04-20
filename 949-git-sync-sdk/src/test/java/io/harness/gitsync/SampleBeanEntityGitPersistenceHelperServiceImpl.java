package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.SampleBean;
import io.harness.common.EntityReference;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.ng.core.EntityDetail;

import com.google.inject.Singleton;
import java.util.function.Supplier;

@Singleton
@OwnedBy(DX)
public class SampleBeanEntityGitPersistenceHelperServiceImpl
    implements GitSdkEntityHandlerInterface<SampleBean, SampleBean> {
  @Override
  public Supplier<SampleBean> getYamlFromEntity(SampleBean entity) {
    return () -> entity;
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.CONNECTORS;
  }

  @Override
  public Supplier<SampleBean> getEntityFromYaml(SampleBean yaml) {
    return () -> yaml;
  }

  @Override
  public EntityDetail getEntityDetail(SampleBean entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.CONNECTORS)
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .identifier(entity.getIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .build())
        .build();
  }

  @Override
  public SampleBean save(SampleBean yaml, String accountIdentifier) {
    return null;
  }

  @Override
  public SampleBean update(SampleBean yaml, String accountIdentifier) {
    return null;
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    return false;
  }
}
