package main

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
	"reflect"
	"strings"
	"testing"
	"time"
	"unicode"

	"io/fs"

	"github.com/gingersnap-project/api/tests/config/cache/v1alpha1"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"
	"sigs.k8s.io/yaml"
)

// Command below generates the set of .pb.go files. .proto comes for the gingersnap-api project
// imported as submodule of this repo.
// The --go_opt=module=.. strips out the default module for the generated files, so files are generated
// in the `gingersnap-api/config/cache/v1alpha` folder in the go module root and can be imported as
// `import "your-module-name/gingersnap-api/config/cache/v1alpha`
//
//go:generate protoc --proto_path=../.. --include_source_info --descriptor_set_out=descriptor --go_out=. --go_opt=Mconfig/cache/v1alpha1/cache.proto=github.com/gingersnap-project/api/tests/config/cache/v1alpha1 --go_opt=Mconfig/cache/v1alpha1/rules.proto=github.com/gingersnap-project/api/tests/config/cache/v1alpha1 --go_opt=paths=source_relative config/cache/v1alpha1/cache.proto config/cache/v1alpha1/rules.proto
//go:generate applygingersnapstyle-gen --rm config/cache/v1alpha1/rules.pb.go config/cache/v1alpha1/zz_rules.pb.go
//go:generate applygingersnapstyle-gen --rm config/cache/v1alpha1/cache.pb.go config/cache/v1alpha1/zz_cache.pb.go
var messageTypes = map[string]func() proto.Message{
	"CacheConf":          func() proto.Message { return &v1alpha1.CacheConf{} },
	"EagerCacheRuleSpec": func() proto.Message { return &v1alpha1.EagerCacheRuleSpec{} },
}
var path = os.Getenv("goOutPath")
var yamlCache = `
cacheSpec:
  deployment:
    resources:
      requests:
        memory: "4Gi"
        cpu: "2"
      limits:
        memory: "8Gi"
        cpu: "4"
  dataSource:
    connectionProperties:
      prop1: value1
      prop2: value2    
eagerCacheRuleSpecs:
  myEagerCacheRule:
    cacheRef:
      name: myCache
      namespace: myNamespace
    tableName: TABLE_EAGER_RULE_2
    key:
      format: "JSON"
      keySeparator: ','
      keyColumns:
        - col2
        - col3
        - col4
    value:
      valueColumns:
        - col6
        - col7
        - col8
lazyCacheRuleSpecs:
  myLazyCacheRule1:
    cacheRef:
      name: myCache
      namespace: myNamespace
    query: select name,surname,address,age from myTable where name='?' and value='?'
`

func TestYamlToProtoToJson(t *testing.T) {
	jsonString, err := yaml.YAMLToJSON([]byte(yamlCache))
	if err != nil {
		t.Error(err)
	}
	cache := &v1alpha1.CacheConf{}
	err = protojson.Unmarshal(jsonString, cache)
	if err != nil {
		t.Error(err)
	}

	readableCache, _ := protojson.Marshal(cache)
	if err := checkJsonAreEquiv(jsonString, readableCache); err != nil {
		t.Error(err)
	}
	// Write to file so it can be used by other testsuites
	if err := writeFile(t, cache); err != nil {
		t.Error(err)
	}
}

func TestMessageFromFiles(t *testing.T) {
	fsys := os.DirFS(path)
	matches, _ := fs.Glob(fsys, "*.json")
	for _, name := range matches {
		typeName := strings.FieldsFunc(name, splitFileNameToType)
		file, _ := fsys.Open(name)
		defer file.Close()
		bytes, _ := io.ReadAll(file)
		msg := messageTypes[typeName[0]]()
		if err := protojson.Unmarshal(bytes, msg); err != nil {
			t.Error(err)
		}
		msg1, _ := protojson.Marshal(msg)
		if err := checkJsonAreEquiv(bytes, msg1); err != nil {
			t.Error(err)
		}
	}
}

func checkJsonAreEquiv(jsonString []byte, readableCache []byte) error {
	var v1, v2 interface{}
	if err := json.Unmarshal([]byte(jsonString), &v1); err != nil {
		return err
	}
	if err := json.Unmarshal([]byte(readableCache), &v2); err != nil {
		return err
	}
	if !reflect.DeepEqual(v1, v2) {
		return fmt.Errorf("JSON are not equivalent, expect:\n%s\ngot\n%s", jsonString, readableCache)
	}
	return nil
}

func writeFile(t *testing.T, msg proto.Message) error {
	nameAndPackage := strings.Split(fmt.Sprintf("%T", msg), ".")
	fullName := fmt.Sprintf("%s/%s_go%d.json", path, nameAndPackage[len(nameAndPackage)-1], time.Now().UnixMilli())
	f, err := os.Create(fullName)
	if err != nil {
		return err
	}
	defer f.Close()
	readableCache, err := protojson.Marshal(msg)
	if err != nil {
		return err
	}
	_, err = f.WriteString(string(readableCache))
	return err
}

func splitFileNameToType(r rune) bool {
	return r == '_' || r == '.' || unicode.IsDigit(r)
}
