package io.harness.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.context.GlobalContext;
import io.harness.context.GlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.PersistentEntity;
import io.harness.queue.Queuable.QueuableKeys;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PrePersist;

import java.util.Date;

@Indexes({
  @Index(options = @IndexOptions(name = "next"),
      fields =
      {
        @Field(QueuableKeys.created)
        , @Field(QueuableKeys.version), @Field(QueuableKeys.earliestGet), @Field(QueuableKeys.running),
            @Field(QueuableKeys.resetTimestamp)
      })
  ,
      @Index(options = @IndexOptions(name = "versioned_extra1"), fields = {
        @Field(QueuableKeys.version), @Field(QueuableKeys.earliestGet), @Field(QueuableKeys.resetTimestamp)
      }), @Index(options = @IndexOptions(name = "versioned_extra2"), fields = {
        @Field(QueuableKeys.version), @Field(QueuableKeys.running), @Field(QueuableKeys.earliestGet)
      })
})
@FieldNameConstants(innerTypeName = "QueuableKeys")
public abstract class Queuable implements PersistentEntity {
  @Getter @Setter @Id private String id;
  @Getter @Setter private boolean running;
  @Getter @Setter private Date resetTimestamp = new Date(Long.MAX_VALUE);
  @Getter @Setter private Date earliestGet = new Date();
  @Getter @Setter private Date created = new Date();
  @Getter @Setter private int retries;
  @Getter @Setter private String version;
  @Getter @Setter private GlobalContext globalContext;

  /**
   * Instantiates a new queuable.
   */
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
