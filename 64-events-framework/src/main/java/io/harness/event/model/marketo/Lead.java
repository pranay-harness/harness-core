package io.harness.event.model.marketo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lead {
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

  private List<String> freemiumProducts;
  private Boolean freemiumAssistedOption;
}
