package client

import (
	"bytes"
	"context"
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/stream"
)

var _ Client = (*HTTPClient)(nil)

const (
	streamEndpoint       = "/stream?accountID=%s&key=%s"
	infoEndpoint         = "/info/stream"
	blobEndpoint         = "/blob?accountID=%s&key=%s"
	uploadLinkEndpoint   = "/blob/link/upload?accountID=%s&key=%s"
	downloadLinkEndpoint = "/blob/link/download?accountID=%s&key=%s"
)

var retryTime = 10 * time.Second

// defaultClient is the default http.Client.
var defaultClient = &http.Client{
	CheckRedirect: func(*http.Request, []*http.Request) error {
		return http.ErrUseLastResponse
	},
}

// New returns a new HTTPClient.
func NewHTTPClient(endpoint, accountID, token string, skipverify bool) *HTTPClient {
	client := &HTTPClient{
		Endpoint:   endpoint,
		AccountID:  accountID,
		Token:      token,
		SkipVerify: skipverify,
	}
	if skipverify {
		client.Client = &http.Client{
			CheckRedirect: func(*http.Request, []*http.Request) error {
				return http.ErrUseLastResponse
			},
			Transport: &http.Transport{
				Proxy: http.ProxyFromEnvironment,
				TLSClientConfig: &tls.Config{
					InsecureSkipVerify: true,
				},
			},
		}
	}
	return client
}

// HTTPClient provides an http service client.
type HTTPClient struct {
	Client     *http.Client
	Endpoint   string // Example: http://localhost:port
	Token      string // Per account token to validate against
	AccountID  string
	SkipVerify bool
}

// Upload uploads the file to remote storage.
func (c *HTTPClient) Upload(ctx context.Context, key string, r io.Reader) error {
	path := fmt.Sprintf(blobEndpoint, c.AccountID, key)
	resp, err := c.retry(ctx, path, "POST", r, nil, true)
	if resp != nil {
		defer resp.Body.Close()
	}
	return err
}

// UploadLink returns a secure link that can be used to
// upload a file to remote storage.
func (c *HTTPClient) UploadLink(ctx context.Context, key string) (*Link, error) {
	path := fmt.Sprintf(uploadLinkEndpoint, c.AccountID, key)
	out := new(Link)
	_, err := c.retry(ctx, path, "POST", nil, out, false)
	return out, err
}

// Download downloads the file from remote storage.
func (c *HTTPClient) Download(ctx context.Context, key string) (io.ReadCloser, error) {
	path := fmt.Sprintf(blobEndpoint, c.AccountID, key)
	resp, err := c.open(ctx, path, "GET", nil)
	return resp.Body, err
}

// DownloadLink returns a secure link that can be used to
// download a file from remote storage.
func (c *HTTPClient) DownloadLink(ctx context.Context, key string) (*Link, error) {
	path := fmt.Sprintf(downloadLinkEndpoint, c.AccountID, key)
	out := new(Link)
	_, err := c.do(ctx, path, "POST", nil, out)
	return out, err
}

// Open opens the data stream.
func (c *HTTPClient) Open(ctx context.Context, key string) error {
	path := fmt.Sprintf(streamEndpoint, c.AccountID, key)
	_, err := c.retry(ctx, path, "POST", nil, nil, false)
	return err
}

// Close closes the data stream.
func (c *HTTPClient) Close(ctx context.Context, key string) error {
	path := fmt.Sprintf(streamEndpoint, c.AccountID, key)
	_, err := c.do(ctx, path, "DELETE", nil, nil)
	return err
}

// Write writes logs to the data stream.
func (c *HTTPClient) Write(ctx context.Context, key string, lines []*stream.Line) error {
	path := fmt.Sprintf(streamEndpoint, c.AccountID, key)
	_, err := c.do(ctx, path, "PUT", &lines, nil)
	return err
}

// Tail tails the data stream.
// TODO: (vistaar) This currently doesn't work since server sends back server-sent events.
// Need to find a library which can parse server sent response. This is not used right now.
func (c *HTTPClient) Tail(ctx context.Context, key string) (<-chan *stream.Line, <-chan error) {
	errc := make(chan error, 1)
	outc := make(chan *stream.Line, 100)

	path := fmt.Sprintf(streamEndpoint, c.AccountID, key)
	res, err := c.open(ctx, path, "GET", nil)
	if err != nil {
		errc <- err
		return outc, errc
	}
	if res.StatusCode > 299 {
		errc <- errors.New("cannot stream repository")
		return outc, errc
	}
	go func(res *http.Response) {
		defer res.Body.Close()
		dec := json.NewDecoder(res.Body)
		for {
			select {
			case <-ctx.Done():
				return
			default:
			}
			line := new(stream.Line)
			err := dec.Decode(line)
			if err == io.EOF {
				return
			} else if err != nil {
				errc <- err
				return
			}
			outc <- line
		}
	}(res)
	return outc, errc
}

// Info returns the stream information.
func (c *HTTPClient) Info(ctx context.Context) (*stream.Info, error) {
	out := new(stream.Info)
	_, err := c.do(ctx, infoEndpoint, "GET", nil, out)
	return out, err
}

func (p *HTTPClient) retry(ctx context.Context, method, path string, in, out interface{}, isOpen bool) (*http.Response, error) {
	for {
		var res *http.Response
		var err error
		if !isOpen {
			res, err = p.do(ctx, method, path, in, out)
		} else {
			res, err = p.open(ctx, method, path, in.(io.Reader))
		}

		// do not retry on Canceled or DeadlineExceeded
		if err := ctx.Err(); err != nil {
			logger.FromContext(ctx).WithError(err).WithField("path", path).Errorln("http: context canceled")
			return res, err
		}

		if res != nil {
			// Check the response code. We retry on 500-range
			// responses to allow the server time to recover, as
			// 500's are typically not permanent errors and may
			// relate to outages on the server side.
			if res.StatusCode > 501 {
				logger.FromContext(ctx).WithError(err).WithField("path", path).Warnln("http: server error: reconnect and retry")
				time.Sleep(retryTime)
				continue
			}
		} else if err != nil {
			logger.FromContext(ctx).WithError(err).WithField("path", path).Warnln("http: request error. Retrying ...")
			time.Sleep(retryTime)
			continue
		}
		return res, err
	}
}

// do is a helper function that posts a signed http request with
// the input encoded and response decoded from json.
func (c *HTTPClient) do(ctx context.Context, path, method string, in, out interface{}) (*http.Response, error) {
	var r io.Reader

	if in != nil {
		buf := new(bytes.Buffer)
		json.NewEncoder(buf).Encode(in)
		r = buf
	}

	endpoint := c.Endpoint + path
	req, err := http.NewRequestWithContext(ctx, method, endpoint, r)
	if err != nil {
		return nil, err
	}

	// the request should include the secret shared between
	// the agent and server for authorization.
	req.Header.Add("X-Harness-Token", c.Token)

	res, err := c.client().Do(req)
	if res != nil {
		defer func() {
			// drain the response body so we can reuse
			// this connection.
			io.Copy(ioutil.Discard, io.LimitReader(res.Body, 4096))
			res.Body.Close()
		}()
	}
	if err != nil {
		return res, err
	}

	// if the response body return no content we exit
	// immediately. We do not read or unmarshal the response
	// and we do not return an error.
	if res.StatusCode == 204 {
		return res, nil
	}

	// else read the response body into a byte slice.
	body, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return res, err
	}

	if res.StatusCode > 299 {
		// if the response body includes an error message
		// we should return the error string.
		if len(body) != 0 {
			out := new(Error)
			if err := json.Unmarshal(body, out); err != nil {
				return res, out
			}
			return res, errors.New(
				string(body),
			)
		}
		// if the response body is empty we should return
		// the default status code text.
		return res, errors.New(
			http.StatusText(res.StatusCode),
		)
	}
	if out == nil {
		return res, nil
	}
	return res, json.Unmarshal(body, out)
}

// helper function to open an http request
func (c *HTTPClient) open(ctx context.Context, path, method string, body io.Reader) (*http.Response, error) {
	endpoint := c.Endpoint + path
	req, err := http.NewRequestWithContext(ctx, method, endpoint, body)
	if err != nil {
		return nil, err
	}
	req.Header.Add("X-Harness-Token", c.Token)
	return c.client().Do(req)
}

// client is a helper function that returns the default client
// if a custom client is not defined.
func (c *HTTPClient) client() *http.Client {
	if c.Client == nil {
		return defaultClient
	}
	return c.Client
}
