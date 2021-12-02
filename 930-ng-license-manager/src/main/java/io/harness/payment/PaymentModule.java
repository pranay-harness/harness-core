package io.harness.payment;

import io.harness.payment.services.PaymentService;
import io.harness.payment.services.impl.PaymentServiceimpl;

import com.google.inject.AbstractModule;

public class PaymentModule extends AbstractModule {
  private static PaymentModule instance;

  public static PaymentModule getInstance() {
    if (instance == null) {
      instance = new PaymentModule();
    }
    return instance;
  }

  private PaymentModule() {}

  @Override
  protected void configure() {
    bind(PaymentService.class).to(PaymentServiceimpl.class);
  }
}
