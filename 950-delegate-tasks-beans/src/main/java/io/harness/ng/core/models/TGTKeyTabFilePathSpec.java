package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.TGTGenerationSpecDTO;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyTabFilePath")
public class TGTKeyTabFilePathSpec extends TGTGenerationSpec {
  private String keyPath;

  @Override
  public TGTGenerationSpecDTO toDTO() {
    return TGTKeyTabFilePathSpecDTO.builder().keyPath(getKeyPath()).build();
  }
}
