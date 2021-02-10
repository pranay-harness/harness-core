package http

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"

	"go.uber.org/zap"
)

const (
	accountID                  string = "accountId"
	authorization              string = "Authorization"
	contentType                string = "Content-Type"
	contentTypeApplicationJSON string = "application/json"
	contentTypeFormURLEncoded  string = "application/x-www-form-urlencoded"
	callgraphPath              string = "uploadCallGraph"
	vcsPath                    string = "getTestsToRun"
)

// NewHTTPClient returns a new HTTPClient.
func NewHTTPClient(endpoint, accountID, token string, log *zap.SugaredLogger) *HTTPClient {
	client := &HTTPClient{
		BaseUrl:   endpoint,
		AccountID: accountID,
		Token:     token,
		Logger:    log,
		Client:    &http.Client{},
	}
	return client
}

type getTestsToRunPayload struct {
	Files string `json:"files"`
}

// HTTPClient provides an http service client.
type HTTPClient struct {
	Client    *http.Client
	BaseUrl   string // Example: http://localhost:port
	Token     string // Per account token to validate calls
	AccountID string
	Logger    *zap.SugaredLogger
}

// GetTestsToRun uploads the file to remote storage.
func (c *HTTPClient) GetTestsToRun(changedFiles string) (string, error) {
	if len(changedFiles) == 0 {
		return "", fmt.Errorf("changed files can't be empty")
	}

	req, err := c.getRequest(changedFiles)

	if err != nil {
		return "", err
	}

	res, err := c.Client.Do(req)

	if err != nil {
		c.Logger.Errorw("failed to fetch tests to run", zap.Error(err))
		return "", fmt.Errorf("Failed to connect to %s", c.BaseUrl+vcsPath)
	}

	// not putting a null check on the res as if err is not null then res will also be nonnull
	if res.StatusCode != 200 {
		c.Logger.Errorw("Call to TI server failed", "res_code", res.StatusCode)
		return "", fmt.Errorf("Call to TI server failed %s", c.BaseUrl+vcsPath)
	}

	testsToExecute, _ := ioutil.ReadAll(res.Body)
	res.Body.Close()

	return string(testsToExecute), nil
}

// getRequest returns a httpRequest object for making calls to TI server
// to fetch the tests to run
func (c *HTTPClient) getRequest(changedFiles string) (*http.Request, error) {
	body := &getTestsToRunPayload{
		Files: changedFiles,
	}

	jsonValue, _ := json.Marshal(body)
	req, err := http.NewRequest("POST", c.BaseUrl+vcsPath, bytes.NewBuffer(jsonValue))

	if err != nil {
		c.Logger.Errorw("Failed to create new httpRequest")
		return nil, err
	}

	req.Header.Set(authorization, c.Token)
	req.Header.Set(contentType, contentTypeApplicationJSON)

	q := req.URL.Query()
	q.Add(accountID, c.AccountID)
	req.URL.RawQuery = q.Encode()

	return req, nil
}

// UploadCallGraph callgraph archive to TI server
func (c *HTTPClient) UploadCallGraph(archivePath string) error {
	callgraphEndpoint := c.BaseUrl + callgraphPath
	c.Logger.Infow("Uploading callgraph tar to TI server", "path", archivePath, "url", callgraphEndpoint)
	request, err := c.newfileUploadRequest(callgraphEndpoint, archivePath)

	if err != nil {
		c.Logger.Errorw("Error in creating file upload request")
		return err
	}

	res, err := c.Client.Do(request)
	res.Body.Close()
	if err != nil {
		c.Logger.Errorw("Uploading callgraph to server failed.", "res_code", res.StatusCode, zap.Error(err))
		return fmt.Errorf("Call to TI server failed %s", callgraphEndpoint)
	}
	c.Logger.Infow("Response code of the call is:", "response code", res.StatusCode)
	return nil
}

// newfileUploadRequest returns a new httpRequest to be used to upload callgraph
// tar file to the TI server
func (c *HTTPClient) newfileUploadRequest(uri string, path string) (*http.Request, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)
	part, err := writer.CreateFormFile("fileName", filepath.Base(path))
	if err != nil {
		return nil, err
	}
	_, err = io.Copy(part, file)

	err = writer.Close()
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest("POST", uri, body)
	req.Header.Set(contentType, writer.FormDataContentType())
	req.Header.Set(authorization, c.Token)

	q := req.URL.Query()
	q.Add(accountID, c.AccountID)
	req.URL.RawQuery = q.Encode()
	return req, err
}
