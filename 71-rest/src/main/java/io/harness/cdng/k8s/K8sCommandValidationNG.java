package io.harness.cdng.k8s;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;
import java.util.function.Consumer;

public class K8sCommandValidationNG extends AbstractDelegateValidateTask {
  public K8sCommandValidationNG(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList("criteria");
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(DelegateConnectionResult.builder().criteria("criteria").validated(true).build());
  }
}
