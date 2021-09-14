/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import static software.wings.sm.StateType.ECS_ROUTE53_DNS_WEIGHT_UPDATE_ROLLBACK;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

import com.github.reinert.jjschema.SchemaIgnore;

public class EcsBGRollbackRoute53DNSWeightState extends EcsBGUpdateRoute53DNSWeightState {
  public EcsBGRollbackRoute53DNSWeightState(String name) {
    super(name, ECS_ROUTE53_DNS_WEIGHT_UPDATE_ROLLBACK.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context, true);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public Integer getTimeoutMillis(ExecutionContext context) {
    return ecsStateHelper.getEcsStateTimeoutFromContext(context, true);
  }

  @Override
  @SchemaIgnore
  public int getRecordTTL() {
    return super.getRecordTTL();
  }

  @Override
  @SchemaIgnore
  public void setRecordTTL(int recordTTL) {
    super.setRecordTTL(recordTTL);
  }

  @Override
  @SchemaIgnore
  public boolean isDownsizeOldService() {
    return super.isDownsizeOldService();
  }

  @Override
  @SchemaIgnore
  public void setDownsizeOldService(boolean downsizeOldService) {
    super.setDownsizeOldService(downsizeOldService);
  }

  @Override
  @SchemaIgnore
  public int getOldServiceDNSWeight() {
    return super.getOldServiceDNSWeight();
  }

  @Override
  @SchemaIgnore
  public void setOldServiceDNSWeight(int oldServiceDNSWeight) {
    super.setOldServiceDNSWeight(oldServiceDNSWeight);
  }

  @Override
  @SchemaIgnore
  public int getNewServiceDNSWeight() {
    return super.getNewServiceDNSWeight();
  }

  @Override
  @SchemaIgnore
  public void setNewServiceDNSWeight(int newServiceDNSWeight) {
    super.setNewServiceDNSWeight(newServiceDNSWeight);
  }
}
