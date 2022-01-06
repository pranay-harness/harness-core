// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

type nopWriter struct {
	data []string
}

// Writer needs to implement io.Writer
type StreamWriter interface {
	Start() error                      // Start watching for logs written into buffer
	Write(p []byte) (n int, err error) // Write log data into a buffer
	Open() error                       // Open remote stream for writing of logs
	Close() error                      // Close remote stream for writing of logs
	Error() error                      // Track if any error was recorded
}

func (*nopWriter) Start() error { return nil }
func (*nopWriter) Open() error  { return nil }
func (*nopWriter) Close() error { return nil }
func (*nopWriter) Error() error { return nil }
func (n *nopWriter) Write(p []byte) (int, error) {
	n.data = append(n.data, string(p))
	return len(p), nil
}

func NopWriter() StreamWriter {
	return new(nopWriter)
}
