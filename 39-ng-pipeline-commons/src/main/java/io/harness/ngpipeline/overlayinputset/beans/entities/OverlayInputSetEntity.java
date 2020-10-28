package io.harness.ngpipeline.overlayinputset.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "OverlayInputSetEntityKeys")
@Entity(value = "inputSetsNG", noClassnameStored = true)
@Document("inputSetsNG")
@TypeAlias("io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity")
@HarnessEntity(exportable = true)
public class OverlayInputSetEntity extends BaseInputSetEntity {
  List<String> inputSetReferences;
}
