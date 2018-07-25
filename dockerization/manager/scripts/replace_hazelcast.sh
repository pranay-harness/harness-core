#!/usr/bin/env bash

if [[ -v "HZ_CLUSTER_NAME" ]]; then
#If cluster name is not found , disable hazelcast .
    sed -i "s|<name>dev|<name>${HZ_CLUSTER_NAME}|" /opt/harness/hazelcast.xml
    sed -i "s|<password>dev-pass|<password>${HZ_CLUSTER_NAME}-pass|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"tag-value\">cluster1|<property name=\"tag-value\">${HZ_CLUSTER_NAME}|" /opt/harness/hazelcast.xml
fi

if [[ -v "MANCENTER_URL" ]]; then
    sed -i "s|<management-center enabled=\"false\">http://localhost:8080/mancenter|<management-center enabled=\"true\">${MANCENTER_URL}|" /opt/harness/hazelcast.xml
fi


if [[ "${DEPLOY_MODE}" == "AWS" ]]; then
    sed -i "s|<property name=\"hazelcast.discovery.enabled\">false|<property name=\"hazelcast.discovery.enabled\">true|" /opt/harness/hazelcast.xml
    if [[ -v "AWS_PROD_ACCESS_KEY" ]]; then
        sed -i "s|<discovery-strategy enabled=\"false\" class=\"com.hazelcast.aws.AwsDiscoveryStrategy\"| <discovery-strategy enabled=\"true\ class=\"com.hazelcast.aws.AwsDiscoveryStrategy\"|" /opt/harness/hazelcast.xml
    fi
    if [[ -v "AWS_PROD_ACCESS_KEY" ]]; then
        sed -i "s|<property name=\"access-key\">my-access-key|<property name=\"access-key\">${AWS_PROD_ACCESS_KEY}|" /opt/harness/hazelcast.xml
    fi

    if [[ -v "AWS_PROD_SECRET_KEY" ]]; then
        sed -i "s|<property name=\"secret-key\">my-secret-key|<property name=\"secret-key\">${AWS_PROD_SECRET_KEY}|" /opt/harness/hazelcast.xml
    fi
    sed -i "s|<property name=\"region\">us-west-1|<property name=\"region\">us-west-2|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"tag-key\">aws-test-cluster|<property name=\"tag-key\">hazelcast-cluster|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"security-group-name\">hazelcast|<property name=\"security-group-name\">harness-manager-servers|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"iam-role\">s3access</property>||" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"host-header\">ec2.amazonaws.com</property>||" /opt/harness/hazelcast.xml
elif [[ "${DEPLOY_MODE}" == "ONPREM" ]]; then
    if [[ -v "TCP_HOSTS_DETAILS" ]]; then
    #explicity set this to false else it will not work. If it is already false, it is noop
        sed -i "s|<property name=\"hazelcast.discovery.enabled\">true|<property name=\"hazelcast.discovery.enabled\">false|" /opt/harness/hazelcast.xml
        sed -i "s|<tcp-ip enabled=\"false\">|<tcp-ip enabled=\"true\">|" /opt/harness/hazelcast.xml
        sed -i "s|<members>tcp-ip-members</members>|<members>${TCP_HOSTS_DETAILS}</members>|" /opt/harness/hazelcast.xml
        sed -i "s|<port auto-increment=\"true\" port-count=\"100\">5701</port>|<port auto-increment=\"true\" port-count=\"100\">${HAZELCAST_PORT}</port>|" /opt/harness/hazelcast.xml
        sed -i "s|<interfaces enabled=\"false\">|<interfaces enabled=\"true\">|" /opt/harness/hazelcast.xml
        sed -i "s|<interface>10.10.1.*</interface>|<interface>${CIDR}</interface>|" /opt/harness/hazelcast.xml
    fi
elif [[ "${DEPLOY_MODE}" == "KUBERNETES" ]]; then
    sed -i "s|<property name=\"hazelcast.discovery.enabled\">false|<property name=\"hazelcast.discovery.enabled\">true|" /opt/harness/hazelcast.xml
    sed -i "s|<discovery-strategy enabled=\"false\" class=\"com.hazelcast.aws.AwsDiscoveryStrategy\"| <discovery-strategy enabled=\"true\" class=\"com.hazelcast.kubernetes.HazelcastKubernetesDiscoveryStrategy\"|" /opt/harness/hazelcast.xml

    sed -i "s|<property name=\"access-key\">my-access-key|<property name=\"service-name\">harness-manager|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"secret-key\">my-secret-key|<property name=\"service-label-name\">hazelcast-cluster|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"region\">us-west-1|<property name=\"service-label-value\">harness-manager|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"tag-key\">aws-test-cluster|<property name=\"namespace\">harness|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"security-group-name\">hazelcast</property>||" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"host-header\">ec2.amazonaws.com</property>||" /opt/harness/hazelcast.xml
    sed -i "s| <property name=\"security-group-name\">hazelcast</property>||" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"iam-role\">s3access</property>||" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"tag-key\">aws-test-cluster</property>||" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"tag-value\">cluster1</property>||" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"hz-port\">5701</property>||" /opt/harness/hazelcast.xml
fi
