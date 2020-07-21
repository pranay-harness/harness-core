package io.harness.delegate.task.azure.response;

import io.harness.azure.model.SubscriptionData;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSListSubscriptionsResponse implements AzureVMSSTaskResponse {
  List<SubscriptionData> subscriptions;
}
