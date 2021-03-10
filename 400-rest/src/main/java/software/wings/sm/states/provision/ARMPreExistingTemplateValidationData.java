package software.wings.sm.states.provision;

import software.wings.api.arm.ARMPreExistingTemplate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ARMPreExistingTemplateValidationData {
  private boolean isValidData;
  private String errorMessage;
  private ARMPreExistingTemplate preExistingTemplate;
}
