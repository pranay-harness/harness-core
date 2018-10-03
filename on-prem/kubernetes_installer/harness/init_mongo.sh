#!/usr/bin/env bash

set -e


function seedData(){
	kubectl exec -it -n harness mongo-0 -- bash -c "mongo  mongodb://mongo-0.mongo,mongo-1.mongo,mongo-2.mongo:27017/harness --eval 'rs.status();'" | grep NotYetInitialized 
	if [[ "$?" == "0" ]];then
		echo "Replica sets are not initialized, initializing replica sets..."
		kubectl exec -it -n harness mongo-0 -- bash -c "mongo mongodb://mongo-0.mongo:27017/ < /scripts/initialize_replicaset.js"
		echo "Sleeping for 30 seconds..."
		sleep 30
	fi
	kubectl exec -it -n harness mongo-0 -- bash -c "mongo  mongodb://mongo-0.mongo,mongo-1.mongo,mongo-2.mongo:27017/ < /scripts/add_first_user.js"
	kubectl exec -it -n harness mongo-0 -- bash -c "mongo  mongodb://mongo-0.mongo,mongo-1.mongo,mongo-2.mongo:27017/ < /scripts/add_learning_engine_secret.js"
}

echo "######################### Mongo Start ##############################"
kubectl apply -f output/harness-mongo.yaml

kubectl get configmaps -n harness scripts-configmap

if [[ $? == 1 ]]; then
    echo "No configs found setting config"
    kubectl apply -f output/harness-configs.yaml
fi

i=$(kubectl get pods --selector=role=mongo -n harness | grep Running | wc -l )
wait=0
while [ $i -lt 3  ] && [ $wait -lt 30 ]
do
	sleep 3
	echo "Waiting for pods to come up, number of pods : $i"
	i=$(kubectl get pods --selector=role=mongo -n harness | grep Running | wc -l )
done

if [[ $i -eq 3 ]]; then
	echo "All mongos are up and running"

	kubectl exec -it -n harness mongo-0 -- bash -c "mongo  mongodb://mongo-0.mongo,mongo-1.mongo,mongo-2.mongo:27017/harness --eval 'rs.status();'" | grep NotYetInitialized 

	if [[ "$?" == "0" ]];then
		echo "Replica sets are not initialized, initializing replica sets..."
		kubectl exec -it -n harness mongo-0 -- bash -c "mongo mongodb://mongo-0.mongo:27017/ < /scripts/initialize_replicaset.js"
		echo "Sleeping for 30 seconds..."
		sleep 30
	fi

	kubectl exec -it -n harness mongo-0 -- bash -c "mongo  mongodb://mongo-0.mongo,mongo-1.mongo,mongo-2.mongo:27017/harness --eval 'db.accounts.count()' " > dbOutput.txt
	accountsize=$(tail -1 dbOutput.txt)
	if [[ "$accountsize" == *"0"* ]];then
		echo "No data found, seeding data with seed data"
		seedData
		rm -f dbOutput.txt
	elif [[ "$accountsize" == *"1"* ]] || [[ "$accountsize" == *"2"* ]];then
		echo "Data found in db, not seeding it again..."
		rm -f dbOutput.txt
	else
		echo "Error verifying installation, please check logs"
		echo $accountsize
		cat dbOutput.txt
		exit 1
	fi

else
	echo "Mongo cluster is not coming up as expected"
	exit 1
fi

echo "######################### Mongo End ##############################"