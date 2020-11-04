package io.harness.ci.beans.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "BuildNumberKeys")
@Entity(value = "buildNumber", noClassnameStored = true)
@StoreIn("harnessci")
@Document("buildNumber")
@HarnessEntity(exportable = true)
public class BuildNumber implements PersistentEntity {
  @Field("pipeline_identifier") private String pipelineIdentifier;
  @Builder.Default private Long buildNumber = 0L;
  @FdIndex private String accountIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  @Id @org.springframework.data.annotation.Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
