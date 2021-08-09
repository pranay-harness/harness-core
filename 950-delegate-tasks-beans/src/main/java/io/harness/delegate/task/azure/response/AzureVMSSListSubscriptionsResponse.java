package io.harness.delegate.task.azure.response;

import io.harness.azure.model.SubscriptionData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSListSubscriptionsResponse implements AzureVMSSTaskResponse {
  List<SubscriptionData> subscriptions;
}
