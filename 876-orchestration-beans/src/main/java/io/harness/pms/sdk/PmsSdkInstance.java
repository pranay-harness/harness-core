package io.harness.pms.sdk;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.pms.contracts.plan.ConsumerConfig;
import io.harness.pms.contracts.plan.SdkModuleInfo;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PmsSdkInstanceKeys")
@Entity(value = "pmsSdkInstances", noClassnameStored = true)
@Document("pmsSdkInstances")
@TypeAlias("pmsSdkInstances")
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.PMS)
public class PmsSdkInstance implements PersistentEntity, UuidAware {
  @Setter @NonFinal @Id @org.mongodb.morphia.annotations.Id String uuid;

  @NotNull @FdUniqueIndex String name;
  Map<String, Set<String>> supportedTypes;
  List<StepInfo> supportedSteps;
  List<StepType> supportedStepTypes;
  SdkModuleInfo sdkModuleInfo;

  ConsumerConfig interruptConsumerConfig;
  ConsumerConfig orchestrationEventConsumerConfig;
  ConsumerConfig facilitatorEventConsumerConfig;
  ConsumerConfig nodeStartEventConsumerConfig;
  ConsumerConfig progressEventConsumerConfig;
  ConsumerConfig nodeAdviseEventConsumerConfig;
  ConsumerConfig nodeResumeEventConsumerConfig;

  @Default @Setter @NonFinal @SchemaIgnore @FdIndex @CreatedDate Long createdAt = System.currentTimeMillis();
  @Setter @NonFinal @SchemaIgnore @NotNull @LastModifiedDate Long lastUpdatedAt;
  @Setter @NonFinal @Version Long version;

  @Default Boolean active = false;
}
