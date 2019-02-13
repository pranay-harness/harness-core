package io.harness.event.model.marketo;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.List;

/**
 * @author rktummala on 11/20/2018
 */
@Data
@Builder
public class Lead {
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
  @Default private String action = "createOnly";
  @Default private String lookupField = "email";
  private List<Input> input;

  @Data
  @Builder
  public static class Input {
    private String email;
    private String firstName;
    private String lastName;
    private String company;
    private String Harness_Account_ID__c_lead;
    private String Free_Trial_Status__c;
    private String Days_Left_in_Trial__c;
  }
}
