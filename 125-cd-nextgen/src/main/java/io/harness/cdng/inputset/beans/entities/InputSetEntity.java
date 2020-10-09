package io.harness.cdng.inputset.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cdng.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CDInputSetEntityKeys")
@Entity(value = "inputSetsNG", noClassnameStored = true)
@Document("inputSetsNG")
@TypeAlias("io.harness.cdng.inputset.beans.entities.CDInputSetEntity")
@HarnessEntity(exportable = true)
public class InputSetEntity extends BaseInputSetEntity {
  private InputSetConfig inputSetConfig;
}
