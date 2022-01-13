/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.handler.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;

/**
 * @author rktummala
 */
@OwnedBy(PL)
@Singleton
public interface Constants {
  String EMAIL_ID = "email_id";
  String ACCOUNT_ID = "account_id";
  String USER_ID = "user_id";
  String CUSTOM_EVENT_NAME = "custom_event";
  String CATEGORY = "category";
  String USER_NAME = "user_name";
  String USER_INVITE_ID = "user_invite_id";
  String COMPANY_NAME = "company_name";
  String TECH_NAME = "tech_name";
  String ACCOUNT_EVENT = "account_event";
  String TECH_CATEGORY_NAME = "tech_category";
  String ORIGINAL_TIMESTAMP_NAME = "originalTimestamp";
  String FREEMIUM_PRODUCTS = "freemiumProducts";
  String FREEMIUM_ASSISTED_OPTION = "freemiumAssistedOption";
  String COUNTRY = "country";
  String STATE = "state";
  String PHONE = "phone";
}
