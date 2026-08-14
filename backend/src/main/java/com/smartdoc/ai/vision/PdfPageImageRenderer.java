package com.smartdoc.ai.vision;

import com.smartdoc.document.InvalidDocumentException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.*;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@Component
public class PdfPageImageRenderer {
    static final int MAX_ENCODED_BYTES=8*1024*1024; static final long MAX_PIXELS=16_000_000L;
    public NormalizedImage render(InputStream source,int pageNumber)throws IOException{
        try(PDDocument document=PDDocument.load(source)){
            if(pageNumber<1||pageNumber>document.getNumberOfPages())throw new InvalidDocumentException("当前页码无效");
            org.apache.pdfbox.pdmodel.common.PDRectangle box=document.getPage(pageNumber-1).getCropBox();
            long width=(long)Math.ceil(box.getWidth()*2),height=(long)Math.ceil(box.getHeight()*2);
            if(width<=0||height<=0||width*height>MAX_PIXELS)throw new InvalidDocumentException("当前页图像尺寸过大");
            BufferedImage image=new PDFRenderer(document).renderImageWithDPI(pageNumber-1,144,ImageType.RGB);
            try{return encodePng(image);}finally{image.flush();}
        }
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
