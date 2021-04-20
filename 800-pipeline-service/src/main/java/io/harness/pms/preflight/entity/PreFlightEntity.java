package io.harness.pms.preflight.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.UuidAware;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.inputset.PipelineInputResponse;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PreFlightEntityKeys")
@Entity(value = "preFlightEntity")
@Document("preFlightEntity")
@TypeAlias("preFlightEntity")
@HarnessEntity(exportable = false)
public class PreFlightEntity implements UuidAware {
  @Id @org.mongodb.morphia.annotations.Id String uuid;

  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;

  @NonNull String pipelineYaml;
  List<PipelineInputResponse> pipelineInputResponse;
  List<ConnectorCheckResponse> connectorCheckResponse;
}
