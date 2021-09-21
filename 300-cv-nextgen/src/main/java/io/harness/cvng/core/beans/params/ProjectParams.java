package io.harness.cvng.core.beans.params;

import io.harness.cvng.core.services.CVNextGenConstants;

import javax.ws.rs.PathParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectParams {
  @PathParam(CVNextGenConstants.ACCOUNT_IDENTIFIER_KEY) @NonNull String accountIdentifier;
  @PathParam(CVNextGenConstants.ORG_IDENTIFIER_KEY) @NonNull String orgIdentifier;
  @PathParam(CVNextGenConstants.PROJECT_IDENTIFIER_KEY) @NonNull String projectIdentifier;
}
