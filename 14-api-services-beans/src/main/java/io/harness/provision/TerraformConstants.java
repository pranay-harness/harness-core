
package io.harness.provision;

public final class TerraformConstants {
  private TerraformConstants() {}

  public static final String RUN_PLAN_ONLY_KEY = "runPlanOnly";
  public static final String INHERIT_APPROVED_PLAN = "inheritApprovedPlan";

  public static final String VARIABLES_KEY = "variables";
  public static final String BACKEND_CONFIGS_KEY = "backend_configs";
  public static final String TARGETS_KEY = "targets";
  public static final String TF_VAR_FILES_KEY = "tf_var_files";
  public static final String WORKSPACE_KEY = "tf_workspace";
  public static final String ENCRYPTED_VARIABLES_KEY = "encrypted_variables";
  public static final String ENCRYPTED_BACKEND_CONFIGS_KEY = "encrypted_backend_configs";
  public static final String TF_NAME_PREFIX = "tfPlan_%s";
  public static final String TF_DESTROY_NAME_PREFIX = "tfDestroyPlan_%s";
  public static final String TF_APPLY_VAR_NAME = "terraformApply";
  public static final String TF_DESTROY_VAR_NAME = "terraformDestroy";
  public static final String QUALIFIER_APPLY = "apply";

  public static final String USER_DIR_KEY = "user.dir";
  public static final String TERRAFORM_STATE_FILE_NAME = "terraform.tfstate";
  public static final String WORKSPACE_DIR_BASE = "terraform.tfstate.d";
  public static final String WORKSPACE_STATE_FILE_PATH_FORMAT = WORKSPACE_DIR_BASE + "/%s/terraform.tfstate";
  public static final String WORKSPACE_PLAN_FILE_PATH_FORMAT = WORKSPACE_DIR_BASE + "/$WORKSPACE_NAME/tfplan";
  public static final String WORKSPACE_DESTROY_PLAN_FILE_PATH_FORMAT =
      WORKSPACE_DIR_BASE + "/$WORKSPACE_NAME/tfdestroyplan";
  public static final String TERRAFORM_PLAN_FILE_OUTPUT_NAME = "tfplan";
  public static final String TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME = "tfdestroyplan";
  public static final String TERRAFORM_APPLY_PLAN_FILE_VAR_NAME = "${terraformApply.tfplan}";
  public static final String TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME = "${terraformDestroy.tfplan}";
  public static final String TERRAFORM_PLAN_FILE_NAME = "terraform.tfplan";
  public static final String TERRAFORM_VARIABLES_FILE_NAME = "terraform-%s.tfvars";
  public static final String TERRAFORM_BACKEND_CONFIGS_FILE_NAME = "backend_configs-%s";
  public static final String TERRAFORM_INTERNAL_FOLDER = ".terraform";
  public static final long RESOURCE_READY_WAIT_TIME_SECONDS = 15;
  public static final String VAR_FILE_FORMAT = " -var-file=\"%s\" ";
  public static final String TF_WORKING_DIR = "./terraform-working-dir/${ACCOUNT_ID}/${ENTITY_ID}";
}
