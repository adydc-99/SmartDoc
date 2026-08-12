package com.smartdoc.document;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class TextDocumentExtractor {
    public String read(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                throw new InvalidDocumentException("文本文件包含 NUL 字节，疑似二进制内容");
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new InvalidDocumentException("文本文件必须使用有效的 UTF-8 编码");
        }
    }
}
