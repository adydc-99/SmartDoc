package com.smartdoc.ai.vision;

import com.smartdoc.document.InvalidDocumentException;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfPageImageRendererTest {
    @Test void rejectsNonFiniteNonPositiveAndOverflowingExtremeCropBoxesBeforeRender() {
        assertThrows(InvalidDocumentException.class,()->PdfPageImageRenderer.validateCropBox(box(Float.NaN,100)));
        assertThrows(InvalidDocumentException.class,()->PdfPageImageRenderer.validateCropBox(box(Float.POSITIVE_INFINITY,100)));
        assertThrows(InvalidDocumentException.class,()->PdfPageImageRenderer.validateCropBox(box(0,100)));
        assertThrows(InvalidDocumentException.class,()->PdfPageImageRenderer.validateCropBox(box(-1,100)));
        assertThrows(InvalidDocumentException.class,()->PdfPageImageRenderer.validateCropBox(box(Float.MAX_VALUE,Float.MAX_VALUE)));
        assertDoesNotThrow(()->PdfPageImageRenderer.validateCropBox(new PDRectangle(612,792)));
    }
    private static PDRectangle box(float width,float height){PDRectangle box=mock(PDRectangle.class);when(box.getWidth()).thenReturn(width);when(box.getHeight()).thenReturn(height);return box;}
}
