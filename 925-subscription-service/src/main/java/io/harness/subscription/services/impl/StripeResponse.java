package io.harness.subscription.services.impl;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeResponse {
  String result;
}
