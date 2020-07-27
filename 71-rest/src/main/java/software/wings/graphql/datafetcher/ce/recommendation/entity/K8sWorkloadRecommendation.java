package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.annotation.StoreIn;
import io.harness.data.structure.MongoMapSanitizer;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PrePersist;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "K8sWorkloadRecommendationKeys")
@StoreIn("events")
@Entity(value = "k8sWorkloadRecommendation", noClassnameStored = true)
@CdUniqueIndex(name = "no_dup",
    fields =
    { @Field("accountId")
      , @Field("clusterId"), @Field("namespace"), @Field("workloadName"), @Field("workloadType") })
public class K8sWorkloadRecommendation
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  private static final MongoMapSanitizer SANITIZER = new MongoMapSanitizer('~');

  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotEmpty String accountId;
  @NotEmpty String clusterId;
  @NotEmpty String namespace;
  @NotEmpty String workloadType;
  @NotEmpty String workloadName;

  @Singular @NotEmpty Map<String, ContainerRecommendation> containerRecommendations;
  @Singular @NotEmpty Map<String, ContainerCheckpoint> containerCheckpoints;

  @FdIndex BigDecimal estimatedSavings;

  @EqualsAndHashCode.Exclude @FdTtlIndex Instant ttl;

  boolean populated;

  @PostLoad
  public void postLoad() {
    for (ContainerRecommendation cr : containerRecommendations.values()) {
      if (cr.getCurrent() != null) {
        cr.setCurrent(ResourceRequirement.builder()
                          .requests(SANITIZER.decodeDotsInKey(cr.getCurrent().getRequests()))
                          .limits(SANITIZER.decodeDotsInKey(cr.getCurrent().getLimits()))
                          .build());
      }
      if (cr.getBurstable() != null) {
        cr.setBurstable(ResourceRequirement.builder()
                            .requests(SANITIZER.decodeDotsInKey(cr.getBurstable().getRequests()))
                            .limits(SANITIZER.decodeDotsInKey(cr.getBurstable().getLimits()))
                            .build());
      }
      if (cr.getGuaranteed() != null) {
        cr.setGuaranteed(ResourceRequirement.builder()
                             .requests(SANITIZER.decodeDotsInKey(cr.getGuaranteed().getRequests()))
                             .limits(SANITIZER.decodeDotsInKey(cr.getGuaranteed().getLimits()))
                             .build());
      }
    }
  }

  @PrePersist
  public void prePersist() {
    for (ContainerRecommendation cr : containerRecommendations.values()) {
      if (cr.getCurrent() != null) {
        cr.setCurrent(ResourceRequirement.builder()
                          .requests(SANITIZER.encodeDotsInKey(cr.getCurrent().getRequests()))
                          .limits(SANITIZER.encodeDotsInKey(cr.getCurrent().getLimits()))
                          .build());
      }
      if (cr.getBurstable() != null) {
        cr.setBurstable(ResourceRequirement.builder()
                            .requests(SANITIZER.encodeDotsInKey(cr.getBurstable().getRequests()))
                            .limits(SANITIZER.encodeDotsInKey(cr.getBurstable().getLimits()))
                            .build());
      }
      if (cr.getGuaranteed() != null) {
        cr.setGuaranteed(ResourceRequirement.builder()
                             .requests(SANITIZER.encodeDotsInKey(cr.getGuaranteed().getRequests()))
                             .limits(SANITIZER.encodeDotsInKey(cr.getGuaranteed().getLimits()))
                             .build());
      }
    }
  }
}
