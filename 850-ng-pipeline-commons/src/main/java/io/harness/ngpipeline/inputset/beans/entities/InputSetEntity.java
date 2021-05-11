package io.harness.ngpipeline.inputset.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "CDInputSetEntityKeys")
@Entity(value = "inputSetsNG", noClassnameStored = true)
@Document("inputSetsNG")
@TypeAlias("io.harness.ngpipeline.inputset.beans.entities.CDInputSetEntity")
@HarnessEntity(exportable = true)
@ToBeDeleted
@Deprecated
public class InputSetEntity extends BaseInputSetEntity {
  private InputSetConfig inputSetConfig;
}
