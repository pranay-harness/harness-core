package io.harness.subscription.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodCollectionDTO {
  List<CardDTO> paymentMethods;
}
