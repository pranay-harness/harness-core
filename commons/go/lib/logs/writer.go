package logs

type nopWriter struct{}

// Writer needs to implement io.Writer
type StreamWriter interface {
	Start() error                      // Start watching for logs written into buffer
	Write(p []byte) (n int, err error) // Write log data into a buffer
	Open() error                       // Open remote stream for writing of logs
	Close() error                      // Close remote stream for writing of logs
}

func (*nopWriter) Start() error                { return nil }
func (*nopWriter) Open() error                 { return nil }
func (*nopWriter) Close() error                { return nil }
func (*nopWriter) Write(p []byte) (int, error) { return len(p), nil }

func NopWriter() StreamWriter {
	return new(nopWriter)
}
