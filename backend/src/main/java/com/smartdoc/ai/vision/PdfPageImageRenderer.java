package com.smartdoc.ai.vision;

import com.smartdoc.document.InvalidDocumentException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.*;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@Component
public class PdfPageImageRenderer {
    static final int MAX_ENCODED_BYTES=8*1024*1024; static final long MAX_PIXELS=16_000_000L;private static final double RENDER_SCALE=144d/72d;
    public NormalizedImage render(InputStream source,int pageNumber)throws IOException{
        try(PDDocument document=PDDocument.load(source)){
            if(pageNumber<1||pageNumber>document.getNumberOfPages())throw new InvalidDocumentException("当前页码无效");
            validateCropBox(document.getPage(pageNumber-1).getCropBox());
            BufferedImage image=new PDFRenderer(document).renderImageWithDPI(pageNumber-1,144,ImageType.RGB);
            try{return encodePng(image);}finally{image.flush();}
        }
    }
    static void validateCropBox(PDRectangle box){
        if(box==null||!Float.isFinite(box.getWidth())||!Float.isFinite(box.getHeight())||box.getWidth()<=0||box.getHeight()<=0)throw new InvalidDocumentException("当前页图像尺寸无效");
        double scaledWidth=Math.ceil(box.getWidth()*RENDER_SCALE),scaledHeight=Math.ceil(box.getHeight()*RENDER_SCALE);
        if(!Double.isFinite(scaledWidth)||!Double.isFinite(scaledHeight)||scaledWidth>Long.MAX_VALUE||scaledHeight>Long.MAX_VALUE)throw new InvalidDocumentException("当前页图像尺寸过大");
        long width=(long)scaledWidth,height=(long)scaledHeight;
        if(width<=0||height<=0||height>MAX_PIXELS||width>MAX_PIXELS/height)throw new InvalidDocumentException("当前页图像尺寸过大");
    }
    static NormalizedImage encodePng(BufferedImage image)throws IOException{
        if((long)image.getWidth()*image.getHeight()>MAX_PIXELS)throw new InvalidDocumentException("图像像素数超过限制");
        CappedOutputStream out=new CappedOutputStream(MAX_ENCODED_BYTES);
        try{if(!ImageIO.write(image,"png",out))throw new InvalidDocumentException("无法编码图像");}
        catch(SizeLimitException e){throw new InvalidDocumentException("图像数据超过 8 MiB 限制");}
        return new NormalizedImage(out.toByteArray(),"image/png");
    }
    private static final class SizeLimitException extends IOException{}
    private static final class CappedOutputStream extends OutputStream{
        private final int cap;private final ByteArrayOutputStream delegate=new ByteArrayOutputStream();CappedOutputStream(int cap){this.cap=cap;}
        @Override public void write(int b)throws IOException{ensure(1);delegate.write(b);} @Override public void write(byte[] b,int off,int len)throws IOException{ensure(len);delegate.write(b,off,len);}
        private void ensure(int count)throws SizeLimitException{if(delegate.size()+count>cap)throw new SizeLimitException();}byte[] toByteArray(){return delegate.toByteArray();}
    }
}
