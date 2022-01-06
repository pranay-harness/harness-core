/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.model.marketo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lead {
  private String email;
  private String firstName;
  private String lastName;
  private String phone;
  private String company;
  private String country;
  private String state;
  private String Harness_Account_ID__c_lead;
  private String Free_Trial_Status__c;
  private String Freemium_Invite_URL__c;
  private String Days_Left_in_Trial__c;
  private String SSO_Freemium_Type__c;

  private String UTM_Source__c;
  private String UTM_Content__c;
  private String UTM_Medium__c;
  private String UTM_Term__c;
  private String UTM__c;

  private String Freemium_Products__c;
  private boolean freemiumassistedoption;
}
