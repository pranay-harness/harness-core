package io.harness.ci.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "BuildNumberDetailsKeys")
@Entity(value = "buildNumberDetails", noClassnameStored = true)
@StoreIn("harnessci")
@Document("buildNumberDetails")
@HarnessEntity(exportable = true)
public class BuildNumberDetails implements PersistentEntity {
  @Field("pipeline_identifier") private String pipelineIdentifier;
  @Builder.Default private Long buildNumber = 0L;
  @FdIndex private String accountIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  @Id @org.springframework.data.annotation.Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
