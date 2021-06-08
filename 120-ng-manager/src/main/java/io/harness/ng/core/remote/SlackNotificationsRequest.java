package io.harness.ng.core.remote;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SlackNotificationsRequest {
    DeploymentDetails deploymentDetails;
    List<User> users;
}
