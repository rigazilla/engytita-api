package org.engytita.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.protobuf.util.JsonFormat;

import config.cache.v1alpha.Cache;

public class AppJsonToProtobuf {
    public static String yaml = "name: cacheExample\n" +
            "namespace: nsExample\n" +
            "regions:\n" +
            "  - name: region1\n" +
            "    datasource: DataSource1\n" +
            "    rule:\n" +
            "      jsonpath:\n" +
            "        value: some.domain.stores\n" +
            "    expiration:\n" +
            "      schedule: 0 0 1 * *\n" +
            "  - name: region2\n" +
            "    datasource: DataSource2\n" +
            "    rule:\n" +
            "      wildcard:\n" +
            "        value: /pets/(.*)\n" +
            "    preload:\n" +
            "      http:\n" +
            "        url: value\n" +
            "      schedule: 0 0 1 * *\n" +
            "    expiration:\n" +
            "      lifespan: 86400s\n" +
            "    bound:\n" +
            "      count:\n" +
            "        value: \"1000\"\n";

    // Conversion yaml->json->protobuf->json
    public static void main(String[] args) {
        Cache.Builder cache = Cache.newBuilder();
        try {
            String json = convertYamlToJson(yaml);
            JsonFormat.parser().merge(json, cache);
            System.out.println(JsonFormat.printer().print(cache.build()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String convertYamlToJson(String yaml) throws JsonMappingException, JsonProcessingException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }
}
