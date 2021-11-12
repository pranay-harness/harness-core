package io.harness.ng.userprofile.commons;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.SourceCodeManagerAuthentication;
import io.harness.mongo.index.FdIndex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@Schema(name = "SourceCodeManager", description = "This is the view of the SourceCodeManager entity defined in Harness")
public abstract class SourceCodeManagerDTO {
  String id;
  String userIdentifier;
  @FdIndex String accountIdentifier;
  @NotNull String name;
  Long createdAt;
  Long lastModifiedAt;
  public abstract SCMType getType();
  public abstract SourceCodeManagerAuthentication getAuthentication();
}