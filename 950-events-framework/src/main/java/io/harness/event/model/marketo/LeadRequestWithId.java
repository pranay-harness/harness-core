/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.model.marketo;

import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * @author rktummala on 11/20/2018
 */
@Data
@Builder
public class LeadRequestWithId {
  //  {
  //    "action":"createOrUpdate",
  //      "lookupField":"email",
  //      "input":[
  //    {
  //      "email": "rama@harness.io",
  //        "firstName": "Rama",
  //        "lastName": "Tummala",
  //        "company": "Test account"
  //    }   ]
  //  }

  /**
   * Valid values are createOnly, updateOnly and createOrUpdate
   */
  @Default private String action = "createOrUpdate";
  @Default private String lookupField = "id";
  private List<LeadWithId> input;

  @Data
  public static class LeadWithId extends Lead {
    private int id;

    @Builder
    public LeadWithId(String email, String firstName, String lastName, String phone, String company, String country,
        String state, String harness_Account_ID__c_lead, String free_Trial_Status__c, String freemium_Invite_URL__c,
        String days_Left_in_Trial__c, String SSO_Freemium_Type__c, String UTM_Source__c, String UTM_Content__c,
        String UTM_Medium__c, String UTM_Term__c, String UTM__c, String freemium_Products__c,
        boolean freemiumassistedoption) {
      super(email, firstName, lastName, phone, company, country, state, harness_Account_ID__c_lead,
          free_Trial_Status__c, freemium_Invite_URL__c, days_Left_in_Trial__c, SSO_Freemium_Type__c, UTM_Source__c,
          UTM_Content__c, UTM_Medium__c, UTM_Term__c, UTM__c, freemium_Products__c, freemiumassistedoption);
      this.id = id;
    }
  }
}
