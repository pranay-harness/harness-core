/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.sm.states.AwsAmiSwitchRoutesState.SWAP_AUTO_SCALING_ROUTES;
import static software.wings.sm.states.EcsBGUpdateListnerState.ECS_UPDATE_LISTENER_COMMAND;
import static software.wings.sm.states.EcsBGUpdateRoute53DNSWeightState.UPDATE_ROUTE_53_DNS_WEIGHTS;
import static software.wings.sm.states.EcsBlueGreenServiceSetup.ECS_SERVICE_SETUP_COMMAND_ELB;
import static software.wings.sm.states.EcsBlueGreenServiceSetupRoute53DNS.ECS_SERVICE_SETUP_COMMAND_ROUTE53;
import static software.wings.sm.states.EcsDaemonServiceSetup.ECS_DAEMON_SERVICE_SETUP_COMMAND;
import static software.wings.sm.states.EcsRunTaskDeploy.ECS_RUN_TASK_COMMAND;
import static software.wings.sm.states.EcsServiceDeploy.ECS_SERVICE_DEPLOY;
import static software.wings.sm.states.EcsServiceSetup.ECS_SERVICE_SETUP_COMMAND;
import static software.wings.sm.states.EcsSetupRollback.ECS_DAEMON_SERVICE_ROLLBACK_COMMAND;
import static software.wings.sm.states.EcsSteadyStateCheck.ECS_STEADY_STATE_CHECK_COMMAND_NAME;
import static software.wings.sm.states.GcbState.GCB_LOGS;
import static software.wings.sm.states.HelmDeployState.HELM_COMMAND_NAME;
import static software.wings.sm.states.JenkinsState.COMMAND_UNIT_NAME;
import static software.wings.sm.states.KubernetesSteadyStateCheck.KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME;
import static software.wings.sm.states.KubernetesSwapServiceSelectors.KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME;
import static software.wings.sm.states.azure.AzureVMSSDeployState.AZURE_VMSS_DEPLOY_COMMAND_NAME;
import static software.wings.sm.states.azure.AzureVMSSSetupState.AZURE_VMSS_SETUP_COMMAND_NAME;
import static software.wings.sm.states.azure.AzureVMSSSwitchRoutesState.AZURE_VMSS_SWAP_ROUTE;
import static software.wings.sm.states.azure.appservices.AzureWebAppSlotSetup.APP_SERVICE_SLOT_SETUP;
import static software.wings.sm.states.azure.appservices.AzureWebAppSlotShiftTraffic.APP_SERVICE_SLOT_TRAFFIC_SHIFT;
import static software.wings.sm.states.azure.appservices.AzureWebAppSlotSwap.APP_SERVICE_SLOT_SWAP;
import static software.wings.sm.states.customdeployment.InstanceFetchState.FETCH_INSTANCE_COMMAND_UNIT;
import static software.wings.sm.states.pcf.MapRouteState.PCF_MAP_ROUTE_COMMAND;
import static software.wings.sm.states.pcf.PcfDeployState.PCF_RESIZE_COMMAND;
import static software.wings.sm.states.pcf.PcfPluginState.PCF_PLUGIN_COMMAND;
import static software.wings.sm.states.pcf.PcfSetupState.PCF_SETUP_COMMAND;
import static software.wings.sm.states.pcf.PcfSwitchBlueGreenRoutes.PCF_BG_SWAP_ROUTE_COMMAND;
import static software.wings.sm.states.provision.ARMStateHelper.AZURE_ARM_COMMAND_UNIT_TYPE;
import static software.wings.sm.states.provision.ARMStateHelper.AZURE_BLUEPRINT_COMMAND_UNIT_TYPE;
import static software.wings.sm.states.provision.TerragruntProvisionState.TERRAGRUNT_PROVISION_COMMAND_UNIT_TYPE;
import static software.wings.sm.states.spotinst.SpotInstDeployState.SPOTINST_DEPLOY_COMMAND;
import static software.wings.sm.states.spotinst.SpotInstListenerUpdateState.SPOTINST_LISTENER_UPDATE_COMMAND;
import static software.wings.sm.states.spotinst.SpotInstServiceSetup.SPOTINST_SERVICE_SETUP_COMMAND;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.Variable;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 11/17/17.
 */
@Data
@Builder
@OwnedBy(CDP)
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(_870_CG_ORCHESTRATION)
public class CommandUnitDetails {
  private String name;
  private CommandExecutionStatus commandExecutionStatus;
  private CommandUnitType commandUnitType;
  @Builder.Default private List<Variable> variables = new ArrayList<>();

  public enum CommandUnitType {
    COMMAND("COMMAND"),
    JENKINS(COMMAND_UNIT_NAME),
    GCB(GCB_LOGS),
    HELM(HELM_COMMAND_NAME),
    KUBERNETES_STEADY_STATE_CHECK(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME),
    ECS_STEADY_STATE_CHECK(ECS_STEADY_STATE_CHECK_COMMAND_NAME),
    PCF_SETUP(PCF_SETUP_COMMAND),
    SPOTINST_SETUP(SPOTINST_SERVICE_SETUP_COMMAND),
    SPOTINST_DEPLOY(SPOTINST_DEPLOY_COMMAND),
    SPOTINST_UPDATE_LISTENER(SPOTINST_LISTENER_UPDATE_COMMAND),
    PCF_RESIZE(PCF_RESIZE_COMMAND),
    PCF_MAP_ROUTE(PCF_MAP_ROUTE_COMMAND),
    PCF_BG_SWAP_ROUTE(PCF_BG_SWAP_ROUTE_COMMAND),
    PCF_PLUGIN(PCF_PLUGIN_COMMAND),
    KUBERNETES_SWAP_SERVICE_SELECTORS(KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME),
    KUBERNETES("KUBERNETES"),
    AWS_AMI_SWITCH_ROUTES(SWAP_AUTO_SCALING_ROUTES),
    AWS_ECS_UPDATE_LISTENER_BG(ECS_UPDATE_LISTENER_COMMAND),
    AWS_ECS_UPDATE_ROUTE_53_DNS_WEIGHT(UPDATE_ROUTE_53_DNS_WEIGHTS),
    AWS_ECS_SERVICE_SETUP(ECS_SERVICE_SETUP_COMMAND),
    AWS_ECS_RUN_TASK_DEPLOY(ECS_RUN_TASK_COMMAND),
    AWS_ECS_SERVICE_SETUP_ROUTE53(ECS_SERVICE_SETUP_COMMAND_ROUTE53),
    AWS_ECS_SERVICE_SETUP_ELB(ECS_SERVICE_SETUP_COMMAND_ELB),
    AWS_ECS_SERVICE_SETUP_DAEMON(ECS_DAEMON_SERVICE_SETUP_COMMAND),
    AWS_ECS_SERVICE_ROLLBACK_DAEMON(ECS_DAEMON_SERVICE_ROLLBACK_COMMAND),
    AWS_ECS_SERVICE_DEPLOY(ECS_SERVICE_DEPLOY),
    AZURE_VMSS_SETUP(AZURE_VMSS_SETUP_COMMAND_NAME),
    AZURE_VMSS_DEPLOY(AZURE_VMSS_DEPLOY_COMMAND_NAME),
    AZURE_VMSS_SWAP(AZURE_VMSS_SWAP_ROUTE),
    AZURE_APP_SERVICE_SLOT_SETUP(APP_SERVICE_SLOT_SETUP),
    AZURE_APP_SERVICE_SLOT_TRAFFIC_SHIFT(APP_SERVICE_SLOT_TRAFFIC_SHIFT),
    AZURE_APP_SERVICE_SLOT_SWAP(APP_SERVICE_SLOT_SWAP),
    CUSTOM_DEPLOYMENT_FETCH_INSTANCES(FETCH_INSTANCE_COMMAND_UNIT),
    AZURE_ARM_DEPLOYMENT(AZURE_ARM_COMMAND_UNIT_TYPE),
    AZURE_BLUEPRINT_DEPLOYMENT(AZURE_BLUEPRINT_COMMAND_UNIT_TYPE),
    TERRAGRUNT_PROVISION(TERRAGRUNT_PROVISION_COMMAND_UNIT_TYPE);

    private String name;

    public String getName() {
      return name;
    }

    CommandUnitType(String name) {
      this.name = name;
    }
  }
}
