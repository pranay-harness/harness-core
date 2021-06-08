package io.harness.ng.core.remote;

import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;

@Data
@Builder
public class DeploymentDetails {
    String deploymentType;
    String deploymentStatus;
    String triggeredBy;
    String triggeredOn;
    String pipelineExecutionLink;
}
