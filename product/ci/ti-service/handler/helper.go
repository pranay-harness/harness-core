// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package handler

import (
	"encoding/json"
	"net/http"
)

const (
	accountIDParam   = "accountId"
	orgIdParam       = "orgId"
	projectIdParam   = "projectId"
	pipelineIdParam  = "pipelineId"
	buildIdParam     = "buildId"
	stageIdParam     = "stageId"
	stepIdParam      = "stepId"
	timeMsParam      = "timeMs"
	reportParam      = "report"
	sortParam        = "sort"
	statusParam      = "status"
	pageSizeParam    = "pageSize"
	pageIndexParam   = "pageIndex"
	orderParam       = "order"
	suiteNameParam   = "suite_name"
	defaultPageSize  = "10"
	defaultPageIndex = "0"
	// test intelligence specific params
	repoParam         = "repo"
	sourceBranchParam = "source"
	targetBranchParam = "target"
	classNameParam    = "class"
	limitParam        = "limit"
	shaParam          = "sha"
)

// writeBadRequest writes the json-encoded error message
// to the response with a 400 bad request status code.
func WriteBadRequest(w http.ResponseWriter, err error) {
	writeError(w, err, 400)
}

// writeNotFound writes the json-encoded error message to
// the response with a 404 not found status code.
func WriteNotFound(w http.ResponseWriter, err error) {
	writeError(w, err, 404)
}

// writeInternalError writes the json-encoded error message
// to the response with a 500 internal server error.
func WriteInternalError(w http.ResponseWriter, err error) {
	writeError(w, err, 500)
}

// writeJSON writes the json-encoded representation of v to
// the response body.
func WriteJSON(w http.ResponseWriter, v interface{}, status int) {
	for k, v := range noCacheHeaders {
		w.Header().Set(k, v)
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	enc := json.NewEncoder(w)
	enc.SetEscapeHTML(false)
	enc.SetIndent("", "  ")
	enc.Encode(v)
}

// writeError writes the json-encoded error message to the
// response.
func writeError(w http.ResponseWriter, err error, status int) {
	out := struct {
		Message string `json:"error_msg"`
	}{err.Error()}
	WriteJSON(w, &out, status)
}
