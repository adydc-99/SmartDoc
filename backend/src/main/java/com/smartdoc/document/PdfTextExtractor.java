package com.smartdoc.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.InputStream;
import java.util.*;

public class PdfTextExtractor {
    public List<PageText> extract(InputStream input) throws Exception {
        try (PDDocument document = PDDocument.load(input)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page);
                pages.add(new PageText(page, stripper.getText(document)));
            }
            return pages;
        }
    }
}
