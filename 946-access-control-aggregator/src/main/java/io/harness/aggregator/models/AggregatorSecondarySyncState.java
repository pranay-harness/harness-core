package io.harness.aggregator.models;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "AggregatorSecondarySyncStateKeys")
@Entity(value = "aggregatorSecondarySyncState", noClassnameStored = true)
@Document("aggregatorSecondarySyncState")
@TypeAlias("aggregatorSecondarySyncState")
@StoreIn(ACCESS_CONTROL)
public class AggregatorSecondarySyncState {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  @FdUniqueIndex private String identifier;
  private SecondarySyncStatus secondarySyncStatus;

  @CreatedDate private Long createdAt;
  @LastModifiedDate private Long lastModifiedAt;
  @Version private Long version;

  public enum SecondarySyncStatus { SECONDARY_SYNC_REQUESTED, SECONDARY_SYNC_RUNNING, SWITCH_TO_PRIMARY_REQUESTED }
}
