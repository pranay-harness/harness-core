package io.harness.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.context.GlobalContext;
import io.harness.context.GlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.PersistentEntity;
import io.harness.queue.Queuable.QueuableKeys;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;

@CdIndex(name = "next4", fields = { @Field(QueuableKeys.topic)
                                    , @Field(QueuableKeys.earliestGet) })
@FieldNameConstants(innerTypeName = "QueuableKeys")
public abstract class Queuable implements PersistentEntity {
  @Getter @Setter @Id private String id;

  @Getter
  @Setter
  // Old earliestGet is an indication of event that is abandoned. No need to keep it around.
  @FdTtlIndex(24 * 60 * 60)
  private Date earliestGet = new Date();

  @Getter @Setter private int retries;
  @Getter @Setter private String topic;
  @Getter @Setter private GlobalContext globalContext;

  protected Queuable() {}

  protected Queuable(Date earliestGet) {
    this.earliestGet = earliestGet;
  }

  @PrePersist
  public void onUpdate() {
    if (id == null) {
      id = generateUuid();
    }
  }

  protected void updateGlobalContext(GlobalContextData globalContextData) {
    if (globalContextData != null) {
      GlobalContextManager.upsertGlobalContextRecord(globalContextData);
    }
  }
}
