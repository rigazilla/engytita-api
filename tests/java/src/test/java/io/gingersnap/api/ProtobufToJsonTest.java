package io.gingersnap.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.gingersnapproject.proto.api.config.v1alpha1.EagerCacheRuleSpec;
import io.gingersnapproject.proto.api.config.v1alpha1.KeyFormat;
import io.gingersnapproject.proto.api.config.v1alpha1.EagerCacheKey;
import io.gingersnapproject.proto.api.config.v1alpha1.NamespacedObjectReference;
import io.gingersnapproject.proto.api.config.v1alpha1.Value;

public class ProtobufToJsonTest {
    public static String eRuleTestCaseJSON= "{\n" +
    "  \"cacheRef\": {\n" +
    "    \"name\": \"myCache\",\n" +
    "    \"namespace\": \"myNamespace\"\n" +
    "  },\n" +
    "  \"tableName\": \"TABLE_EAGER_RULE_1\",\n" +
    "  \"key\": {\n" +
    "    \"format\": \"JSON\",\n" +
    "    \"keySeparator\": \",\",\n" +
    "    \"keyColumns\": [\"col1\", \"col3\", \"col4\"]\n" +
    "  },\n" +
    "  \"value\": {\n" +
    "    \"valueColumns\": [\"col6\", \"col7\", \"col8\"]\n" +
    "  }\n" +
    "}";

    public static String eRuleTestCase2JSON= "{\n" +
    "  \"cacheRef\": {\n" +
    "    \"name\": \"myCache\",\n" +
    "    \"namespace\": \"myNamespace\"\n" +
    "  },\n" +
    "  \"tableName\": \"TABLE_EAGER_RULE_2\",\n" +
    "  \"key\": {\n" +
    "    \"format\": \"JSON\",\n" +
    "    \"keySeparator\": \",\",\n" +
    "    \"keyColumns\": [\"colA\", \"colB\", \"colC\"]\n" +
    "  },\n" +
    "  \"value\": {\n" +
    "    \"valueColumns\": [\"col6\", \"col7\", \"col8\"]\n" +
    "  }\n" +
    "}";

    private static EagerCacheRuleSpec eRule;

    @BeforeAll
    public static void init() {
        var eRuleBuilder = EagerCacheRuleSpec.newBuilder();
        var ns = NamespacedObjectReference.newBuilder().setName("myCache").setNamespace("myNamespace");
        // Populating resources
        // Populating key
        var keyColumns = Arrays.asList("col1", "col3", "col4");
        var keyBuilder = EagerCacheKey.newBuilder()
                .setFormat(KeyFormat.JSON)
                .setKeySeparator(",")
                .addAllKeyColumns(keyColumns);
        // Populating value
        var valueColumns = Arrays.asList("col6", "col7", "col8");
        var valueBuilder = Value.newBuilder()
                .addAllValueColumns(valueColumns);
        // Assembling Eager Rule
        eRuleBuilder.setTableName("TABLE_EAGER_RULE_1");
        // Adding key
        eRuleBuilder.setKey(keyBuilder);
        // Adding value
        eRuleBuilder.setValue(valueBuilder);
        // Adding ref to the cache
        eRuleBuilder.setCacheRef(ns);
        eRule = eRuleBuilder.build();
    }

    @Test
    public void EagerRuleTest() throws Exception{
        var eRuleBuilder = EagerCacheRuleSpec.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(eRuleTestCaseJSON, eRuleBuilder);
        EagerCacheRuleSpec eRuleFromJson = eRuleBuilder.build();
        assertEquals(eRule, eRuleFromJson);

        eRuleBuilder = EagerCacheRuleSpec.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(eRuleTestCase2JSON, eRuleBuilder);
        var eRule2FromJson = eRuleBuilder.build();
        assertNotEquals(eRule, eRule2FromJson);

    }

    @Test
    void writeRulesToFile() throws IOException {
        EagerCacheRuleSpec.Builder eRuleBuilder = EagerCacheRuleSpec.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(eRuleTestCaseJSON, eRuleBuilder);
        writeRuleToFile(eRuleBuilder.build());

        eRuleBuilder = EagerCacheRuleSpec.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(eRuleTestCaseJSON, eRuleBuilder);
        writeRuleToFile(eRuleBuilder.build());
    }

    private static void writeRuleToFile(Message m) throws InvalidProtocolBufferException, IOException {
        var fileName = m.getClass().getSimpleName() + "_java"+ System.currentTimeMillis();
        var pathStr = System.getProperty("javaOutPath");
        if (!pathStr.equals("")) {
            var path = Paths.get(pathStr+"/" + fileName+".json");
            Files.writeString(path, JsonFormat.printer().print(m), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testFromFileTest() throws Exception{
        var dirFiles = new File(System.getProperty("javaOutPath"));
        var listOfFiles = dirFiles.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File arg0, String arg1) {
                return arg1.endsWith(".json");
            }});
        for (File file : listOfFiles) {
            var className = file.getName().split("[._0-9]")[0];
            Class<?> clazz = null;
            Message.Builder builder;
            try {
                String fullName = "io.gingersnapproject.proto.api.config.v1alpha1."+className;
                clazz = Class.forName(fullName);
                var json = Files.readString(file.toPath());
                Method m = clazz.getMethod("newBuilder");
                builder = (Message.Builder)m.invoke(m);
                JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
                Message eRule = builder.build();
                var eRuleJson = JsonFormat.printer().print(eRule);
                assertEquals(JsonParser.parseString(json),JsonParser.parseString(eRuleJson));
                writeRuleToFile(eRule);
            } catch (Exception e) {
                continue;
            }
        }
    }
}
