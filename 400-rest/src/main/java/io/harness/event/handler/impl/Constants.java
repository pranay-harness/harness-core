package io.harness.event.handler.impl;

import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;

import static io.harness.annotations.dev.HarnessTeam.PL;

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
