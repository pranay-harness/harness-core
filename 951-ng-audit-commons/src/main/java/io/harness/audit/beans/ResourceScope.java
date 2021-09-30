package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.KeyValuePair;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder(builderClassName = "Builder")
@JsonInclude(NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ResourceScopeKeys")
public class ResourceScope {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @Size(max = 10) List<KeyValuePair> labels;
}
