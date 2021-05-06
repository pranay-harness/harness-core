package io.harness.ngpipeline.overlayinputset.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;

import java.util.List;
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
@FieldNameConstants(innerTypeName = "OverlayInputSetEntityKeys")
@Entity(value = "inputSetsNG", noClassnameStored = true)
@Document("inputSetsNG")
@TypeAlias("io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity")
@HarnessEntity(exportable = true)
@ToBeDeleted
@Deprecated
public class OverlayInputSetEntity extends BaseInputSetEntity {
  List<String> inputSetReferences;
}
