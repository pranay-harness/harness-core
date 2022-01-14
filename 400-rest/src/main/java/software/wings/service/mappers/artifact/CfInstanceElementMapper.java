package software.wings.service.mappers.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;

import software.wings.api.PcfInstanceElement;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
public class CfInstanceElementMapper {
  public PcfInstanceElement toPcfInstanceElement(CfInternalInstanceElement internalInstanceElement) {
    return PcfInstanceElement.builder()
        .uuid(internalInstanceElement.getUuid())
        .applicationId(internalInstanceElement.getApplicationId())
        .instanceIndex(internalInstanceElement.getInstanceIndex())
        .displayName(internalInstanceElement.getDisplayName())
        .isUpsize(internalInstanceElement.isUpsize())
        .build();
  }

  public List<PcfInstanceElement> toPcfInstanceElements(List<CfInternalInstanceElement> internalInstanceElement) {
    return internalInstanceElement.stream()
        .map(CfInstanceElementMapper::toPcfInstanceElement)
        .collect(Collectors.toList());
  }
}
