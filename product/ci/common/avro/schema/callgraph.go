package schema

import (
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
	"strings"
)

func bindata_read(data []byte, name string) ([]byte, error) {
	gz, err := gzip.NewReader(bytes.NewBuffer(data))
	if err != nil {
		return nil, fmt.Errorf("Read %q: %v", name, err)
	}

	var buf bytes.Buffer
	_, err = io.Copy(&buf, gz)
	gz.Close()

	if err != nil {
		return nil, fmt.Errorf("Read %q: %v", name, err)
	}

	return buf.Bytes(), nil
}

var _callgraph_avsc = []byte("\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\xff\xdc\x93\x3d\x6e\xc3\x30\x0c\x85\x77\x9f\x82\xe0\x6c\xe4\x00\x5e\x7b\x81\xa2\x6b\x91\x81\x95\xe9\x58\xa8\x2c\x19\xa2\x3a\x04\x46\xee\x5e\xc8\x8e\x52\xcb\xd6\x10\x38\x5b\x35\x49\x14\xdf\xfb\x48\xfd\x4c\x15\x00\x00\x5a\x1a\x18\x1b\xc0\x77\xf2\x6c\x03\xd6\x4b\x34\x5c\x47\xc6\x06\x3d\x2b\xe7\xdb\x14\x8c\xa9\x32\x92\x62\x84\x06\x30\xe8\x53\x4f\xde\xb2\xc8\x49\xbb\x94\xd2\x69\x36\xad\x60\xf3\x39\x2f\xe3\x98\x1e\xb3\x15\x0e\xad\x6b\x59\xee\xa2\xc7\xde\x02\xcd\x05\xab\x0d\x40\xf2\x9e\xae\x58\x03\xec\x53\x74\xe0\x41\x4a\xe2\x35\xf5\xad\xd7\xa6\xdd\x50\x37\x90\xbc\xe5\x5d\xd2\xbe\xbf\xed\x98\x16\x58\x3c\xa1\x81\x43\xef\x5a\xac\xef\xde\x31\x24\xc1\x6b\x7b\xc1\x5b\xd9\x7e\xa3\x1f\x49\x7d\xd3\x85\x8f\x1b\xe8\x1c\xae\x6d\x78\x9a\xec\x69\x90\xe3\x60\x65\x48\x5e\x90\xcf\xaa\x43\xea\xc8\x26\x63\xe4\x83\x3b\xc3\x2a\x68\x67\x33\x9f\x2f\xe7\x0c\x93\x7d\xce\xa8\xd3\xa6\x5c\x45\x51\x7b\xde\x45\xf3\xbc\xbf\xd5\x0a\x5e\xfe\x1c\x9e\x0d\xc5\xd2\xff\xfd\x07\x11\xf7\xe3\x15\x1f\x7c\xa3\x81\x25\x64\x6f\x6c\x4a\x15\xa6\x53\x48\x3d\x2f\xa6\x2f\x5f\x5b\x9a\xcc\x91\x33\x54\xb7\xea\x37\x00\x00\xff\xff\x4f\x46\xe9\x41\x43\x05\x00\x00")

func callgraph_avsc() ([]byte, error) {
	return bindata_read(
		_callgraph_avsc,
		"callgraph.avsc",
	)
}

// Asset loads and returns the asset for the given name.
// It returns an error if the asset could not be found or
// could not be loaded.
func Asset(name string) ([]byte, error) {
	cannonicalName := strings.Replace(name, "\\", "/", -1)
	if f, ok := _bindata[cannonicalName]; ok {
		return f()
	}
	return nil, fmt.Errorf("Asset %s not found", name)
}

// AssetNames returns the names of the assets.
func AssetNames() []string {
	names := make([]string, 0, len(_bindata))
	for name := range _bindata {
		names = append(names, name)
	}
	return names
}

// _bindata is a table, holding each asset generator, mapped to its name.
var _bindata = map[string]func() ([]byte, error){
	"callgraph.avsc": callgraph_avsc,
}

// AssetDir returns the file names below a certain
// directory embedded in the file by go-bindata.
// For example if you run go-bindata on data/... and data contains the
// following hierarchy:
//     data/
//       foo.txt
//       img/
//         a.png
//         b.png
// then AssetDir("data") would return []string{"foo.txt", "img"}
// AssetDir("data/img") would return []string{"a.png", "b.png"}
// AssetDir("foo.txt") and AssetDir("notexist") would return an error
// AssetDir("") will return []string{"data"}.
func AssetDir(name string) ([]string, error) {
	node := _bintree
	if len(name) != 0 {
		cannonicalName := strings.Replace(name, "\\", "/", -1)
		pathList := strings.Split(cannonicalName, "/")
		for _, p := range pathList {
			node = node.Children[p]
			if node == nil {
				return nil, fmt.Errorf("Asset %s not found", name)
			}
		}
	}
	if node.Func != nil {
		return nil, fmt.Errorf("Asset %s not found", name)
	}
	rv := make([]string, 0, len(node.Children))
	for name := range node.Children {
		rv = append(rv, name)
	}
	return rv, nil
}

type _bintree_t struct {
	Func     func() ([]byte, error)
	Children map[string]*_bintree_t
}

var _bintree = &_bintree_t{nil, map[string]*_bintree_t{
	"callgraph.avsc": &_bintree_t{callgraph_avsc, map[string]*_bintree_t{}},
}}
