# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

REGIONS=`aws ec2 describe-regions | grep RegionName | awk '{ print $2 }' | sed 's/.$//' | sed 's/^.//'`

for REGION in $REGIONS
do
    echo $REGION
    aws ec2 describe-instances --filter "Name=instance-state-name,Values=running" --region $REGION |\
      grep InstanceId | awk '{ print $2 }' | sed 's/.\{2\}$//' | sed 's/^.//' |\
      while read INSTANCE
      do
        echo "  $INSTANCE"
        # aws ec2 modify-instance-attribute --region $REGION --no-disable-api-termination --instance-id=$INSTANCE
        aws ec2 terminate-instances --region $REGION --instance-id=$INSTANCE
      done
done
