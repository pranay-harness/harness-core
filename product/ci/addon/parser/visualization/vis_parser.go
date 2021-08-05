package visualization

import (
	"bufio"
	"fmt"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"
	"go.uber.org/zap"
	"strconv"
	"strings"
)

//Parser reads visgraph file, processes it to extract visgraph
type Parser interface {
	// Parse and read the file from
	Parse(file []string) (*Visgraph, error)
}

// CallGraphParser struct definition
type VisGraphParser struct {
	log *zap.SugaredLogger    // logger
	fs  filesystem.FileSystem // filesystem handler
}

// NewVisGraphParser creates a new CallGraphParser client and returns it
func NewVisGraphParser(log *zap.SugaredLogger, fs filesystem.FileSystem) *VisGraphParser {
	return &VisGraphParser{
		log: log,
		fs:  fs,
	}
}

// iterate through all the cg files in the directory, parse each of them and return Callgraph object
func (cg *VisGraphParser) Parse(files []string) (*Visgraph, error) {
	var finalVg []string
	for _, file := range files {
		f, err := cg.fs.Open(file)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("failed to open file %s", file))
		}
		r := bufio.NewReader(f)
		cgStr, err := rFile(r)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("failed to parse file %s", file))
		}
		cg.log.Infof("successfully parsed vg file %s", file)
		finalVg = append(finalVg, cgStr...)
	}
	return parseString(finalVg)
}

func parseString(vg []string) (*Visgraph, error) {
	var keys, values []cgp.Node

	for _, row := range vg {
		s := strings.Split(row, "|")
		key, value, err := getNodes(s)
		if err != nil {
			return nil, err
		}
		keys = append(keys, *key)
		values = append(values, *value)
	}
	return &Visgraph{
		Keys:   keys,
		Values: values,
	}, nil
}

// Sample data:
// -1149206571|Source|io.harness.cf|ApiClient|setUserAgent|(java.lang.String)|771920609|Source|io.harness.cf|ApiClient|addDefaultHeader|(java.lang.String,java.lang.String)
func getNodes(s []string) (*cgp.Node, *cgp.Node, error) {
	// each line is supposed to have 12 entries
	if len(s) != 12 {
		return nil, nil, fmt.Errorf("parsing failed: string format is not correct %v", s)
	}
	keyId, err1 := strconv.Atoi(s[0])
	valId, err2 := strconv.Atoi(s[6])
	if err1 != nil || err2 != nil {
		return nil, nil, fmt.Errorf("parsing failed: Id format is not correct %s %s", s[0], s[6])
	}
	key := cgp.NewNode(keyId, s[1], s[2], s[3], s[4], s[5])
	val := cgp.NewNode(valId, s[7], s[8], s[9], s[10], s[11])
	return key, val, nil
}

// rFile reads visgraph file
func rFile(r *bufio.Reader) ([]string, error) {
	var ret []string
	s, e := rLine(r)
	for e == nil {
		ret = append(ret, s)
		s, e = rLine(r)
	}
	return ret, nil
}

// rLine reads line in visgraph file which corresponds to one entry of visgraph
// had to use bufio reader as the scanner.Scan() fn has limitation
// over the number of bytes it can read and was not working on visgraph file.
func rLine(r *bufio.Reader) (string, error) {
	var (
		isPrefix bool  = true
		err      error = nil
		line, ln []byte
	)
	for isPrefix && err == nil {
		line, isPrefix, err = r.ReadLine()
		ln = append(ln, line...)
	}
	return string(ln), err
}
