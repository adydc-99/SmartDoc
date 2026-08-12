package com.smartdoc.document;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentContentControllerTest {
    @ParameterizedTest
    @CsvSource({
            "Demo.java,java", "layout.xml,xml", "config.yml,yaml", "config.yaml,yaml", "schema.sql,sql",
            "app.js,javascript", "types.ts,typescript", "data.json,json", "app.properties,properties",
            "build.sh,shell", "setup.ps1,powershell", "README.md,markdown", "GUIDE.markdown,markdown", "notes.txt,text"
    })
    void mapsEverySupportedTextExtensionExplicitly(String filename,String expected) {
        assertEquals(expected,DocumentContentController.language(filename));
    }
}
