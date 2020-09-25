package io.harness.cdng.inputset.beans.resource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@ApiModel("MergeInputSetRequest")
public class MergeInputSetRequestDTO {
  @NonNull @NotEmpty List<String> inputSetIdentifierList;
}
