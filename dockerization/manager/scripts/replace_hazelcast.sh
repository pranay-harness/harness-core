#!/usr/bin/env bash

if [[ -v "HZ_CLUSTER_NAME" ]]; then
#If cluster name is not found , disable hazelcast .
    sed -i "s|<property name=\"hazelcast.discovery.enabled\">false|<property name=\"hazelcast.discovery.enabled\">true|" /opt/harness/hazelcast.xml
    sed -i "s|<name>dev|<name>${HZ_CLUSTER_NAME}|" /opt/harness/hazelcast.xml
    sed -i "s|<password>dev-pass|<password>${HZ_CLUSTER_NAME}-pass|" /opt/harness/hazelcast.xml
    sed -i "s|<property name=\"tag-value\">cluster1|<property name=\"tag-value\">${HZ_CLUSTER_NAME}|" /opt/harness/hazelcast.xml
fi

if [[ -v "MANCENTER_URL" ]]; then
    sed -i "s|<management-center enabled=\"false\">http://localhost:8080/mancenter|<management-center enabled=\"true\">${MANCENTER_URL}|" /opt/harness/hazelcast.xml
fi
sed -i "s|<port auto-increment=\"true\" port-count=\"100\"|<port auto-increment=\"false\" port-count=\"1\"|" /opt/harness/hazelcast.xml
sed -i "s|<discovery-strategy enabled=\"false\"|<discovery-strategy enabled=\"true\"|" /opt/harness/hazelcast.xml

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