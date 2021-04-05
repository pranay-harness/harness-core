package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.KeyValuePair;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@Builder
@JsonInclude(NON_NULL)
@FieldNameConstants(innerTypeName = "ResourceKeys")
public class Resource {
  @NotNull @NotEmpty String type;
  @NotNull @NotEmpty String identifier;
  List<KeyValuePair> labels;
}
