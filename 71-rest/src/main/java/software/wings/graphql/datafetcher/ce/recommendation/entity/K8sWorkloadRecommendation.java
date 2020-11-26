package software.wings.graphql.datafetcher.ce.recommendation.entity;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotation.StoreIn;
import io.harness.ccm.commons.beans.HarnessServiceInfo;
import io.harness.data.structure.MongoMapSanitizer;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
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

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "K8sWorkloadRecommendationKeys")
@StoreIn("events")
@Entity(value = "k8sWorkloadRecommendation", noClassnameStored = true)
@NgUniqueIndex(name = "no_dup",
    fields =
    { @Field("accountId")
      , @Field("clusterId"), @Field("namespace"), @Field("workloadName"), @Field("workloadType") })
public final class K8sWorkloadRecommendation
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

  // Timestamp at which we last sampled util data for this workload
  // max(lastSampleStart) across containerCheckpoints
  Instant lastReceivedUtilDataAt;

  // Timestamp at which we last computed recommendations for this workload
  Instant lastComputedRecommendationAt;

  // For intermediate stages in batch-processing
  boolean dirty;

  // Set to true if we have non-empty recommendations
  boolean validRecommendation;

  // To avoid showing recommendation if cost computation cannot be done due to lastDay's cost not being available
  boolean lastDayCostAvailable;

  // number of days of data (min across containers)
  int numDays;

  HarnessServiceInfo harnessServiceInfo;

  @PostLoad
  public void postLoad() {
    if (containerRecommendations != null) {
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
        if (cr.getRecommended() != null) {
          cr.setRecommended(ResourceRequirement.builder()
                                .requests(SANITIZER.decodeDotsInKey(cr.getRecommended().getRequests()))
                                .limits(SANITIZER.decodeDotsInKey(cr.getRecommended().getLimits()))
                                .build());
        }
      }
    }
  }

  @PrePersist
  public void prePersist() {
    // set validRecommendation to false in case empty recommendation
    validRecommendation = false;
    boolean noDiffInAllContainers = true;
    if (containerRecommendations != null) {
      validRecommendation = true;
      for (ContainerRecommendation cr : containerRecommendations.values()) {
        if (!isEmpty(cr.getCurrent())) {
          cr.setCurrent(ResourceRequirement.builder()
                            .requests(SANITIZER.encodeDotsInKey(cr.getCurrent().getRequests()))
                            .limits(SANITIZER.encodeDotsInKey(cr.getCurrent().getLimits()))
                            .build());
        }
        if (isEmpty(cr.getBurstable())) {
          validRecommendation = false;
        } else {
          cr.setBurstable(ResourceRequirement.builder()
                              .requests(SANITIZER.encodeDotsInKey(cr.getBurstable().getRequests()))
                              .limits(SANITIZER.encodeDotsInKey(cr.getBurstable().getLimits()))
                              .build());
          if (!Objects.equals(cr.getCurrent(), cr.getBurstable())) {
            noDiffInAllContainers = false;
          }
        }
        if (isEmpty(cr.getGuaranteed())) {
          validRecommendation = false;
        } else {
          cr.setGuaranteed(ResourceRequirement.builder()
                               .requests(SANITIZER.encodeDotsInKey(cr.getGuaranteed().getRequests()))
                               .limits(SANITIZER.encodeDotsInKey(cr.getGuaranteed().getLimits()))
                               .build());
          if (!Objects.equals(cr.getCurrent(), cr.getGuaranteed())) {
            noDiffInAllContainers = false;
          }
        }
        if (isEmpty(cr.getRecommended())) {
          validRecommendation = false;
        } else {
          cr.setRecommended(ResourceRequirement.builder()
                                .requests(SANITIZER.encodeDotsInKey(cr.getRecommended().getRequests()))
                                .limits(SANITIZER.encodeDotsInKey(cr.getRecommended().getLimits()))
                                .build());
          if (!Objects.equals(cr.getCurrent(), cr.getRecommended())) {
            noDiffInAllContainers = false;
          }
        }
      }
    }
    if (noDiffInAllContainers) {
      validRecommendation = false;
    }
  }
}
