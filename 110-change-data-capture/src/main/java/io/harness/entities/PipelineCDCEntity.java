package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.changehandlers.TagsInfoCDChangeDataHandler;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.pipeline.PipelineEntity;

import com.google.inject.Inject;

public class PipelineCDCEntity implements CDCEntity<PipelineEntity> {
  @Inject private TagsInfoCDChangeDataHandler tagsInfoCDChangeDataHandler;

  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    if (handlerClass.contentEquals("TagsInfoCD")) {
      return tagsInfoCDChangeDataHandler;
    }
    return null;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return PipelineEntity.class;
  }
}
