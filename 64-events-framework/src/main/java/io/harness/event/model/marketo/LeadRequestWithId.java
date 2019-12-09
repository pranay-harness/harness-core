package io.harness.event.model.marketo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.List;

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
  private List<Lead> input;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Lead {
    private int id;
    private String email;
    private String firstName;
    private String lastName;
    private String company;
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
  }
}
