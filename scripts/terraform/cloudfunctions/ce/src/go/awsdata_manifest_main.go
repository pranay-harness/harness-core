// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Package cloudFunction contains a Google Cloud Storage Cloud Function which handle AWS -> BQ Ingestion
package cloudFunction

import (
	"cloud.google.com/go/bigquery"
	"cloud.google.com/go/scheduler/apiv1"
	"cloud.google.com/go/storage"
	"context"
	"encoding/json"
	"fmt"
	"google.golang.org/api/iterator"
	schedulerpb "google.golang.org/genproto/googleapis/cloud/scheduler/v1"
	"io/ioutil"
	"log"
	"os"
	"regexp"
	"strconv"
	"strings"
	"time"
)

// GCSEvent is the payload of a GCS event. Please refer to the docs for
// additional information regarding GCS events.
type GCSEvent struct {
	Bucket string `json:"bucket"`
	Name   string `json:"name"`
}

//Column : The Columns in the Manifest Json
type Column struct {
	Category string `json:"category"`
	Name     string `json:"name"`
	DataType string `json:"type"`
}

//BillingPeriod : The BillingPeriod Details in the Manifest Json
type BillingPeriod struct {
	Start string `json:"start"`
	End   string `json:"end"`
}

type AdditionalArtifact struct {
	ArtifactType string `json:"artifactType"`
	Name         string `json:"name"`
}

//ManifestJson : The Json Structure AWS publishes in the CUR report
type manifestJSON struct {
	AssemblyID             string               `json:"assemblyId"`
	Account                string               `json:"account"`
	Columns                []Column             `json:"columns"`
	Charset                string               `json:"charset"`
	Compression            string               `json:"compression"`
	ContentType            string               `json:"contentType"`
	ReportID               string               `json:"reportId"`
	ReportName             string               `json:"reportName"`
	BillingPeriod          BillingPeriod        `json:"billingPeriod"`
	Bucket                 string               `json:"bucket"`
	ReportKeys             []string             `json:"reportKeys"`
	AdditionalArtifactKeys []AdditionalArtifact `json:"additionalArtifactKeys"`
}

//CreateTable : is responsible for creating a Table in BigQuery
func CreateTable(ctx context.Context, e GCSEvent) error {
	ctxBack := context.Background()
	projectId := os.Getenv("GCP_PROJECT")
	if len(projectId) == 0 {
		projectId = "ce-prod-274307"
	}
	client, bigQueryErr := bigquery.NewClient(ctxBack, projectId)
	if bigQueryErr != nil {
		log.Fatal("error creating client: {}", bigQueryErr.Error())
	}
	storageClient, errStorage := storage.NewClient(ctxBack)
	if errStorage != nil {
		log.Fatal("error creating Storage client: {}", errStorage.Error())
	}
	PathSlice := strings.Split(e.Name, "/")
	awsRoleIdWithaccountIdSlice := strings.Split(PathSlice[0], ":")
	accountIdOrig := awsRoleIdWithaccountIdSlice[len(awsRoleIdWithaccountIdSlice)-1]
	accountId := strings.ToLower(awsRoleIdWithaccountIdSlice[len(awsRoleIdWithaccountIdSlice)-1])
	inValidRegex, _ := regexp.Compile("[^a-z0-9_]")
	if inValidRegex.MatchString(accountId) {
		accountId = inValidRegex.ReplaceAllString(accountId, "_")
	}
	datasetName := "BillingReport_" + accountId
	// 4th from left is definite to be the date folder
	DateFolderSlice := strings.Split(PathSlice[3], "-")
	TableDateSuffix := DateFolderSlice[0][:4] + "_" + DateFolderSlice[0][4:6]
	tableName := "awsCurTable_" + TableDateSuffix

	if !strings.HasSuffix(e.Name, ".json") {
		fmt.Println("Not a JSON file. Exiting :", e.Name)
		return nil
	}
	fmt.Println("Processing JSON from path:", e.Name)
	rc, readerClientErr := storageClient.Bucket(e.Bucket).Object(e.Name).NewReader(ctxBack)
	// log.Printf("Printing Event Context %+v\n", e)
	if readerClientErr != nil {
		fmt.Println(readerClientErr)
		return nil
	}
	defer rc.Close()
	fmt.Println("Reading data from manifest")
	body, readingErr := ioutil.ReadAll(rc)
	if readingErr != nil {
		fmt.Println(readingErr)
		return nil
	}
	var jsonData manifestJSON
	if readingErr = json.Unmarshal(body, &jsonData); readingErr != nil {
		fmt.Println("Could not Deserialise Manifest File: {}", readingErr.Error())
		return nil
	} else if jsonData.Columns == nil {
		fmt.Println("Could not Deserialise Manifest File. No columns found. Exiting")
		return nil
	}

	createDataSet := true
	datasetsIterator := client.Datasets(ctxBack)
	for {
		dataset, err := datasetsIterator.Next()
		if err == iterator.Done {
			break
		}
		if dataset.DatasetID == "BillingReport_"+accountId {
			createDataSet = false
			break
		}
	}

	if createDataSet {
		dataSetMetaData := &bigquery.DatasetMetadata{
			Location: "US",
		}
		if dataSetCreationErr := client.Dataset("BillingReport_"+accountId).Create(ctxBack, dataSetMetaData); dataSetCreationErr != nil {
			fmt.Println("Error Creating DataSet:", dataSetCreationErr.Error())
		}
	}

	CloudFunctionsDataset := client.Dataset(datasetName)
	CloudFunctionsDataset.Table(tableName).Delete(ctxBack)
	if tableCreateErr := CloudFunctionsDataset.Table(tableName).Create(ctxBack, &bigquery.TableMetadata{Schema: getSchema(jsonData)}); tableCreateErr != nil {
		fmt.Println("Error Creating table. This error can be ignored.", tableCreateErr.Error())
	}

	// CloudScheduler Code
	fmt.Println("Processing CloudScheduler code")
	ctxBack = context.Background()
	c, err := scheduler.NewCloudSchedulerClient(ctxBack)
	if err != nil {
		fmt.Println(err)
		return err
	}
	msgData := make(map[string]string)
	msgData["accountId"] = accountId
	msgData["accountIdOrig"] = accountIdOrig
	msgData["bucket"] = e.Bucket
	msgData["fileName"] = e.Name
	msgData["datasetName"] = datasetName
	msgData["tableName"] = tableName
	msgData["projectId"] = projectId
	msgData["tableSuffix"] = TableDateSuffix
	msgDataJson, _ := json.Marshal(msgData)
	msgDataString := string(msgDataJson)

	triggerTime := time.Now().UTC()
	triggerTime = triggerTime.Add(20 * time.Minute) // after 10 mins
	schedule := fmt.Sprintf("%d %d %d %d *", triggerTime.Minute(), triggerTime.Hour(), triggerTime.Day(), int(triggerTime.Month()))

	topic := fmt.Sprintf("projects/%s/topics/ce-awsdata-scheduler", projectId)
	fmt.Println("Topic: ", topic)
	fmt.Println("msgDataString:", msgDataString)
	var pubsubtarget = &schedulerpb.PubsubTarget{
		TopicName: topic,
		Data:      []byte(msgDataString),
	}

	var jobTarget = &schedulerpb.Job_PubsubTarget{
		PubsubTarget: pubsubtarget,
	}
	paths := strings.Split(e.Name, "/")
	// It is quite definite that 4th from left will be the month folder always.
	month := paths[3]
	var name string
	if strings.HasSuffix(paths[4], ".json") {
		name = fmt.Sprintf("projects/%s/locations/us-central1/jobs/ce-awsdata-%s-%s", projectId, accountId, month)
	} else if strings.HasSuffix(paths[5], ".json") {
		subfolder := paths[4]
		name = fmt.Sprintf("projects/%s/locations/us-central1/jobs/ce-awsdata-%s-%s-%s", projectId, accountId, month, subfolder)
	}
	description := fmt.Sprintf("Scheduler for %s", accountId)
	var job = &schedulerpb.Job{
		Name:        name,
		Description: description,
		Target:      jobTarget,
		Schedule:    schedule,
		TimeZone:    "UTC",
	}

	// https://godoc.org/google.golang.org/genproto/googleapis/cloud/scheduler/v1#DeleteJobRequest
	/*
	  req1 := &schedulerpb.DeleteJobRequest{
	    Name: name,
	  }
	  err = c.DeleteJob(ctxBack, req1)
	  if err != nil {
	    fmt.Printf("Error while trying to delete older schedules. This can be ignored. %s\n", err)
	  }
	*/

	// https://godoc.org/google.golang.org/genproto/googleapis/cloud/scheduler/v1#DeleteJobRequest
	req1 := &schedulerpb.UpdateJobRequest{
		Job: job,
	}
	_, err = c.UpdateJob(ctxBack, req1)
	if err != nil {
		fmt.Printf("No older schedules found to update. This can be ignored. %s\n", err)
		req := &schedulerpb.CreateJobRequest{
			Parent: fmt.Sprintf("projects/%s/locations/us-central1", projectId),
			Job:    job,
		}
		_, err = c.CreateJob(ctxBack, req)
		if err != nil {
			fmt.Printf("%s\n", err)
			return err
		}
		fmt.Printf("Created new schedule\n")
	}
	fmt.Printf("Updated older schedule\n")

	// Delete manifest only when scheduler is set
	if DeleteObjectErr := storageClient.Bucket(e.Bucket).Object(e.Name).Delete(ctxBack); DeleteObjectErr != nil {
		fmt.Println("Error Deleting Json Manifest File Post Processing:", DeleteObjectErr.Error())
	}
	fmt.Println("Processed and deleted JSON:", e.Name)
	return nil
}

//getSchema Method creates the Schema of the BQ table
func getSchema(jsonData manifestJSON) bigquery.Schema {
	var columnsArray []*bigquery.FieldSchema
	mapOfNames := make(map[string]int)
	for _, column := range jsonData.Columns {
		name := strings.ToLower(column.Name)
		// Handle Tag based rows which have `:` [Tags]
		inValidRegex, _ := regexp.Compile("[^a-zA-Z0-9_]")
		if inValidRegex.MatchString(column.Name) {
			name = "TAG_" + inValidRegex.ReplaceAllString(column.Name, "_")
		}
		nameReferenceForMapCount := strings.ToLower(name)
		NoOfOccurences, isPresent := mapOfNames[nameReferenceForMapCount]
		if !isPresent {
			mapOfNames[nameReferenceForMapCount] = 1
		} else {
			// Handle the case-insesitive nature of BQ columns
			log.Print("Name: "+name+" Occurence? ", NoOfOccurences)
			mapOfNames[nameReferenceForMapCount] = NoOfOccurences + 1
			name = name + "_" + strconv.Itoa(NoOfOccurences)
		}
		dataType := getMappedDataColumn(column.DataType)
		column := &bigquery.FieldSchema{
			Required: false,
			Name:     name,
			Type:     dataType,
		}
		columnsArray = append(columnsArray, column)
	}
	return bigquery.Schema(columnsArray)
}

//getMappedDataColumn get the BQ compliant DataType
func getMappedDataColumn(dataType string) bigquery.FieldType {
	var modifiedDataType bigquery.FieldType
	switch dataType {
	case "String":
		modifiedDataType = bigquery.StringFieldType
	case "OptionalString":
		modifiedDataType = bigquery.StringFieldType
	case "Interval":
		modifiedDataType = bigquery.StringFieldType
	case "DateTime":
		modifiedDataType = bigquery.TimestampFieldType
	case "BigDecimal":
		modifiedDataType = bigquery.FloatFieldType
	case "OptionalBigDecimal":
		modifiedDataType = bigquery.FloatFieldType
	default:
		modifiedDataType = bigquery.StringFieldType
	}
	return modifiedDataType
}
