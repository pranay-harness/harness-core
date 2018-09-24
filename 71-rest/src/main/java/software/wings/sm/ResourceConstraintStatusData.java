package software.wings.sm;

import lombok.Builder;
import lombok.Value;
import software.wings.waitnotify.NotifyResponseData;

@Value
@Builder
public class ResourceConstraintStatusData implements NotifyResponseData {}