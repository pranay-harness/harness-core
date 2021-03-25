package io.harness.cvng.beans.job;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.cvng.beans.DataSourceType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants(innerTypeName = "VerificationJobDTOKeys")
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@SuperBuilder
public abstract class VerificationJobDTO {
  private String identifier;
  private String jobName;
  private String serviceIdentifier;
  private String serviceName;
  private String envIdentifier;
  private String envName;
  private String projectIdentifier;
  private String orgIdentifier;
  private String activitySourceIdentifier;
  private List<DataSourceType> dataSources;
  private List<String> monitoringSources;
  private String verificationJobUrl;
  // TODO: make it Duration and write a custom serializer
  private String duration;
  private boolean isDefaultJob;

  public abstract VerificationJobType getType();

  public static boolean isRuntimeParam(String value) {
    return hasSome(value) && value.equals("<+input>");
  }
}
