/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp.procurement;

public enum ProcurementEventType {
  ACCOUNT_CREATION_REQUESTED, // Deprecated
  ACCOUNT_ACTIVE, // Indicates that the customer's account has been created.
  ACCOUNT_DELETED, // Indicates that the customer's account was deleted from Google Cloud systems.
  ENTITLEMENT_CREATION_REQUESTED, // Indicates that a customer selected one of your pricing plans.
  ENTITLEMENT_ACTIVE, // Indicates that a customer's chosen plan is now active.
  ENTITLEMENT_PLAN_CHANGE_REQUESTED, // Indicates that a customer chose a new plan.
  ENTITLEMENT_PLAN_CHANGED, // Indicates that a customer's plan change is approved and the changes have taken effect.
  ENTITLEMENT_PLAN_CHANGE_CANCELLED, // Indicates that a customer's plan change was canceled, either because it wasn't
                                     // approved, or they switched back to their old plan.
  ENTITLEMENT_PENDING_CANCELLATION, // Indicates that a customer cancelled their plan, and the cancellation is pending
                                    // until the end of the billing cycle.
  ENTITLEMENT_CANCELLATION_REVERTED, // Indicates that a customer's pending cancellation was reverted. Note that
                                     // cancellations cannot be reverted after they are final.
  ENTITLEMENT_CANCELLED, // Indicates that a customer's plan was cancelled.
  ENTITLEMENT_CANCELLING, // Indicates that a customer's plan is in the process of being cancelled.
  ENTITLEMENT_DELETED // Indicates that information about a customer's plan was deleted from Google Cloud Marketplace.
}
