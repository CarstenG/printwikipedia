package wikitopdf.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.html.simpleparser.StyleSheet;
import com.lowagie.text.pdf.FontSelector;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.MultiColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import wikitopdf.utils.WikiLogger;
import wikitopdf.utils.WikiSettings;
import wikitopdf.html.WHTMLWorker;
import wikitopdf.html.WikiHtmlConverter;
import wikitopdf.html.WikiStyles;
import wikitopdf.wiki.WikiPage;

/**
 * @author Denis Lunev <den.lunev@gmail.com>
 */
public class PdfPageWrapper {

    /**
     *
     * @param index
     * @throws DocumentException
     * @throws IOException
     */
    public PdfPageWrapper(int index, int cVolNum, int pageNum) throws DocumentException, IOException { 
        //Read settings.
        //'_' - prefix for for temp file. After stamping file would be renamed
        System.out.println("i am start page " + pageNum);
        outputFileName = "_" + index + "-" + cVolNum + "-" + pageNum +"-"+ WikiSettings.getInstance().getOutputFileName();
        System.out.println(outputFileName);
        outputFileName = outputFileName.replace("/","\\");
        prefn = "/../copyright/pre"+String.format("%04d", cVolNum)+".pdf";
//      WHTMLWorker.fontGet();//start font thing for the new page.
        tFontGet();//title/entryheading font
        fontGet();//regular font
        preFontGet();//smaller font for quotes/<pre> tags. -- not sure if this is working or being rendered.

        pdfDocument = new Document(new Rectangle(432, 648));//6" x 9"

        preDoc = new Document(new Rectangle(432,648));
        
        pdfDocument.setMargins(67, 47.5f, -551, 49.5f); //old margins w/error.
//        pdfDocument.setMargins(66.3f, 47f, 5.5f, 62.5f);//toc margins 
        pdfWriter = PdfWriter.getInstance(pdfDocument,
                new FileOutputStream( WikiSettings.getInstance().getOutputFolder() +
                            "/" + outputFileName));
        preWriter = PdfWriter.getInstance(preDoc,
                new FileOutputStream( WikiSettings.getInstance().getOutputFolder() +
                            "/" + prefn));

        header = new PageHeaderEvent(pageNum,pdfDocument);
        pdfWriter.setPageEvent(header);

        pdfDocument.open();

        _wikiFontSelector = new WikiFontSelector();
        
        
        pdfDocument.setMarginMirroring(true);//for alternating margins for alternate pages

          addPrologue(cVolNum, preDoc, preWriter); //creates copyright two pages.
        
        openMultiColumn();
    }

    
    
    public void openMultiColumn() {

        mct = new MultiColumnText(600);
        int columnCount = 3;
        float space = (float) 8;
        float columnWidth = (float) 103;
        float left = 67;
        float right = left + columnWidth;

        mct.addRegularColumns(pdfDocument.left(), pdfDocument.right(), 6f, 3);

        //First page hack
        for (int i = 0; i < 38; i++) {//same as the TOC -- for some reason the first entry always wants to start at the top of the page. This moves it down. Should be a better fix.
            try {
                Phrase ph = _wikiFontSelector.getTitleFontSelector().process("\n");
                mct.addElement(ph);
                pdfDocument.add(mct);
            } catch (Exception ex) {
                WikiLogger.getLogger().severe(currentTitle + " - Error: " + ex.getMessage());
            }
        }
    }
    
    public final void addPrologue(int cVolNum, Document docu, PdfWriter writ) throws DocumentException {
        //adds copyright document.
        if(docu.equals(preDoc)){
            preDoc.open();
            preDoc.setMarginMirroring(true);
        }
        PdfContentByte cb = writ.getDirectContent();
        BaseFont times = null;
        try {
            _wikiFontSelector.getTitleFontSelector().process("");
            times = _wikiFontSelector.getCommonFont().getBaseFont();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            bflib = BaseFont.createFont("fonts/LinLibertine_Rah.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (IOException ex) {
            Logger.getLogger(PdfPageWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        int c = 57391;
        String wiki_w = Character.toString((char)c);

        //wikipedia on the first inside page.--right facing
        cb.beginText();
        cb.setFontAndSize(bflib, 42);
        cb.setTextMatrix(docu.right() - 182, 425);
        cb.showText(wiki_w+"ikipedia");
        cb.endText();
        PdfPTable tocTable = new PdfPTable(1);
        
        try {
            //setting volume number position and adding to page.
            _wikiFontSelector.getTitleFontSelector().process("");
            times = _wikiFontSelector.getCommonFont().getBaseFont();
            Font pght = new Font(bflib,16);
            Paragraph pgh = new Paragraph("\nVolume "+String.valueOf(cVolNum),pght);
            PdfPCell cell = new PdfPCell(pgh);
            cell.setBorderWidth(0f);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setColspan(1);
            tocTable.addCell(cell);
            ColumnText column = new ColumnText(writ.getDirectContent());
            column.addElement(tocTable);
            column.setSimpleColumn(docu.left()+15, docu.bottom()+20, docu.right()+27.5f, docu.bottom()-100);
            column.go();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        docu.newPage();//get second page for the copyright text
//        jknewPage(docu);

        String copyrightText = "CC BY-SA 3.0 2015, Wikipedia contributors; see Appendix for a complete list of contributors. Please see http://creativecommons.org/licenses/by-sa/3.0/ for full license.\r\r"+
                "Edited, compiled and designed by Michael Mandiberg (User:Theredproject).\r\r"+ 
                "This work is legally categorized as an artistic work. As such, this qualifies for trademark use under clause 3.6.3 (Artistic, scientific, literary, political, and other non-commercial uses) as denoted at wikimediafoundation.org/wiki/Trademark_policy\r\r"+
                "Wikipedia is a trademark of the Wikimedia Foundation and is used with the permission of the Wikimedia Foundation. This work is not endorsed by or affiliated with the Wikimedia Foundation.\r\r"+
                "Cover set in Linux Libertine. Book set in Cardo, with the following 36 typefaces added to handle the many languages contained within: Alef, Amiri, Android Emoji, Bitstream CyberCJK, Casy EA, cwTeXFangSong, cwTeXHei, cwTeXKai, cwTeXMing, cwTeXYen, DejaVu Sans, Droid Arabic Kufi, FreeSans, FreeSerif, GurbaniAkharSlim, IndUni-N, Junicode, Lohit Gujarati, Lohit Oriya, MAC C Times, MS Gothic, NanumGothic, Noto Kufi Arabic, Noto Sans, Noto Sans Bengali, Noto Sans Cherokee, Noto Sans Devanagari, Noto Sans Georgian, Noto Sans Japanese, Noto Sans Sinhala, Noto Sans Tamil UI, Noto Sans Telugu, Noto Sans Thai, Noto Serif Armenian, Open Sans, Roboto.\r\r"+
                "Produced with support from Eyebeam, The Banff Centre, the City University of New York, and Lulu.com. Designed and built with assistance from Denis Lunev, Jonathan Kirtharan, Kenny Lozowski, Patrick Davison, and Colin Elliot.\r\r"+
                "PrintWikipedia.com\r\rGitHub.com/mandiberg/printwikipedia\r\rPrinted by Lulu.com";
        PdfPTable cpTable = new PdfPTable(1);
        try { //setting copyright text and adding to page
            times = _wikiFontSelector.getCommonFont().getBaseFont();
            Font cpt = new Font(bflib,8);
            Paragraph cpp = new Paragraph(copyrightText,cpt);
            PdfPCell cell2 = new PdfPCell(cpp);
            cell2.setLeading(10,0);
            cell2.setBorderWidth(0f);
            cell2.setHorizontalAlignment(Element.ALIGN_TOP);
            cell2.setVerticalAlignment(Element.ALIGN_LEFT);
            cell2.setColspan(1);
            cpTable.addCell(cell2);
            ColumnText column2 = new ColumnText(writ.getDirectContent());
            column2.addElement(cpTable);
            column2.setSimpleColumn (docu.left()-30, 10, docu.right(), docu.top()-15);
            column2.go();
            
            docu.newPage();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        if(docu.equals(preDoc)){
            preDoc.close();
            File savedithink = new File(prefn);
        }
        
    }

    /**
     *
     * @param page
     */
    public void tFontGet(){
        String path_to_fonts = "/Users/wiki/repos/printwikipedia/dist/fonts/";
        FontFactory.register(path_to_fonts+"Cardo104s.ttf","cardo");
        FontFactory.register(path_to_fonts+"msgothic.tcc.0","fontGlyph");
        FontFactory.register(path_to_fonts+"cwTeXFangSong-zhonly.ttf","chinese1");
        FontFactory.register(path_to_fonts+"cwTeXHei-zhonly.ttf","chinese2");
        FontFactory.register(path_to_fonts+"cwTeXKai-zhonly.tff","chinese3");
        FontFactory.register(path_to_fonts+"cwTeXMing-zhonly.ttf","chinese4");
        FontFactory.register(path_to_fonts+"cwTeXYen-zhonly.ttf","chinese5");
        FontFactory.register(path_to_fonts+"G5LISL1B.TTF","chinese6");
        FontFactory.register(path_to_fonts+"Amiri-Regular.ttf","arab1");
        FontFactory.register(path_to_fonts+"DroidKufi-Regular.ttf","arab2");
        FontFactory.register(path_to_fonts+"Alef-Regular.ttf","hebrew");
        FontFactory.register(path_to_fonts+"NotoSansCherokee-Regular.ttf","cherokee");
        FontFactory.register(path_to_fonts+"NotoSansGeorgian-Regular.ttf","georgian");
        FontFactory.register(path_to_fonts+"NotoSansDevanagari-Regular.ttf","devanagari");
        FontFactory.register(path_to_fonts+"NanumGothic-Regular.ttf","nanum");
        FontFactory.register(path_to_fonts+"NotoKufiArabic-Regular.ttf","arab3");
        FontFactory.register(path_to_fonts+"NotoSansJP-Regular.otf","jap");
        FontFactory.register(path_to_fonts+"NotoSansKhmer-Regular.ttf","khmer");
        FontFactory.register(path_to_fonts+"NotoSansThai-Regular.ttf","thai");
        FontFactory.register(path_to_fonts+"NotoSerifArmenian-Regular.ttf","armenian");
        FontFactory.register(path_to_fonts+"NotoSansTamilUI-Regular.ttf","tamil");
        FontFactory.register(path_to_fonts+"DejaVuSans.ttf","dvs");
        FontFactory.register(path_to_fonts+"Roboto-Regular.ttf","roboto");
        FontFactory.register(path_to_fonts+"OpenSans-Light.ttf","ops");
        FontFactory.register(path_to_fonts+"MCTIME.TTF","russ");
        FontFactory.register(path_to_fonts+"FreeSerif.ttf","fser");
        FontFactory.register(path_to_fonts+"NotoSansSinhala-Regular.ttf","sinhala");
        FontFactory.register(path_to_fonts+"NotoSansBengali-Regular.ttf","bengali");
        FontFactory.register(path_to_fonts+"lohit_gu.ttf","punj");
        FontFactory.register(path_to_fonts+"FreeSans.ttf","fsans");
        FontFactory.register(path_to_fonts+"NotoSansTelugu-Regular.ttf","telugu");
        FontFactory.register(path_to_fonts+"Cybercjk.ttf","cjk");
        FontFactory.register(path_to_fonts+"IndUni-N-Roman.otf","ind");
        FontFactory.register(path_to_fonts+"lohit_or.ttf","oriya");
        FontFactory.register(path_to_fonts+"AppleColorEmoji.ttf","emojiAp");
        FontFactory.register(path_to_fonts+"android-emoji.ttf","emojiAn");
        FontFactory.register(path_to_fonts+"casy_ea.ttf","garif");
        
        

        int font_size = 13;
        Font cardo = FontFactory.getFont("cardo", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font roboto = FontFactory.getFont("roboto", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font russ = FontFactory.getFont("russ", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese1 = FontFactory.getFont("chinese1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese2 = FontFactory.getFont("chinese2", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese3 = FontFactory.getFont("chinese3", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese4 = FontFactory.getFont("chinese4", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese5 = FontFactory.getFont("chinese5", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese6 = FontFactory.getFont("chinese6", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab1 = FontFactory.getFont("arab1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab2 = FontFactory.getFont("arab2", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab3 = FontFactory.getFont("arab3", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font hebrew = FontFactory.getFont("hebrew", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font cherokee = FontFactory.getFont("cherokee", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font georgian = FontFactory.getFont("georgian", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font devanagari = FontFactory.getFont("devanagari", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font nanum = FontFactory.getFont("nanum", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font jap = FontFactory.getFont("jap", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font khmer = FontFactory.getFont("khmer", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font thai = FontFactory.getFont("thai", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font tamil = FontFactory.getFont("tamil", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font armenian = FontFactory.getFont("armenian", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font sinhala = FontFactory.getFont("sinhala", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font ops = FontFactory.getFont("ops", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font bengali = FontFactory.getFont("bengali", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font punj = FontFactory.getFont("punj", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fsans = FontFactory.getFont("fsans", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font dvs = FontFactory.getFont("dvs", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);                    
        Font telugu = FontFactory.getFont("telugu", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font cjk = FontFactory.getFont("cjk", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font ind = FontFactory.getFont("ind", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);                    
        Font oriya = FontFactory.getFont("oriya", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fser = FontFactory.getFont("fser", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fontGlyph = FontFactory.getFont("fontGlyph", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font emojiAn = FontFactory.getFont("emojiAn", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font emojiAp = FontFactory.getFont("emojiAp", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font garif = FontFactory.getFont("garif", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        

        tfs.addFont(cardo);
        tfs.addFont(fontGlyph);
        //fs.addFont(dvs);
        tfs.addFont(fser);
        tfs.addFont(cjk);
        tfs.addFont(russ);
        tfs.addFont(armenian);
        tfs.addFont(chinese1);
        tfs.addFont(chinese2);
        tfs.addFont(chinese3);
        tfs.addFont(chinese4);
        tfs.addFont(chinese5);
        tfs.addFont(chinese6);
        tfs.addFont(arab1);
        tfs.addFont(arab2);
        tfs.addFont(arab3);
//                    fs.addFont(ind);
        tfs.addFont(hebrew);
        tfs.addFont(cherokee);
        tfs.addFont(georgian);  
        tfs.addFont(devanagari);
        tfs.addFont(nanum);
        tfs.addFont(jap);
        tfs.addFont(khmer);
        tfs.addFont(thai);
        tfs.addFont(tamil);
        tfs.addFont(ops);
//        fs.addFont(helv);
        tfs.addFont(roboto);
        tfs.addFont(sinhala);
        tfs.addFont(bengali);
        tfs.addFont(punj);
        tfs.addFont(fsans);
        tfs.addFont(telugu);
        tfs.addFont(oriya);
        tfs.addFont(fser);
//        tfs.addFont(emojiAn);
//        tfs.addFont(emojiAp);
        tfs.addFont(garif);
    }
    public void preFontGet(){
        String path_to_fonts = "/Users/wiki/repos/printwikipedia/dist/fonts/";
        FontFactory.register(path_to_fonts+"Cardo104s.ttf","cardo");
        FontFactory.register(path_to_fonts+"msgothic.tcc.0","fontGlyph");
        FontFactory.register(path_to_fonts+"cwTeXFangSong-zhonly.ttf","chinese1");
        FontFactory.register(path_to_fonts+"cwTeXHei-zhonly.ttf","chinese2");
        FontFactory.register(path_to_fonts+"cwTeXKai-zhonly.tff","chinese3");
        FontFactory.register(path_to_fonts+"cwTeXMing-zhonly.ttf","chinese4");
        FontFactory.register(path_to_fonts+"cwTeXYen-zhonly.ttf","chinese5");
        FontFactory.register(path_to_fonts+"G5LISL1B.TTF","chinese6");
        FontFactory.register(path_to_fonts+"Amiri-Regular.ttf","arab1");
        FontFactory.register(path_to_fonts+"DroidKufi-Regular.ttf","arab2");
        FontFactory.register(path_to_fonts+"Alef-Regular.ttf","hebrew");
        FontFactory.register(path_to_fonts+"NotoSansCherokee-Regular.ttf","cherokee");
        FontFactory.register(path_to_fonts+"NotoSansGeorgian-Regular.ttf","georgian");
        FontFactory.register(path_to_fonts+"NotoSansDevanagari-Regular.ttf","devanagari");
        FontFactory.register(path_to_fonts+"NanumGothic-Regular.ttf","nanum");
        FontFactory.register(path_to_fonts+"NotoKufiArabic-Regular.ttf","arab3");
        FontFactory.register(path_to_fonts+"NotoSansJP-Regular.otf","jap");
        FontFactory.register(path_to_fonts+"NotoSansKhmer-Regular.ttf","khmer");
        FontFactory.register(path_to_fonts+"NotoSansThai-Regular.ttf","thai");
        FontFactory.register(path_to_fonts+"NotoSerifArmenian-Regular.ttf","armenian");
        FontFactory.register(path_to_fonts+"NotoSansTamilUI-Regular.ttf","tamil");
        FontFactory.register(path_to_fonts+"DejaVuSans.ttf","dvs");
        FontFactory.register(path_to_fonts+"Roboto-Regular.ttf","roboto");
        FontFactory.register(path_to_fonts+"OpenSans-Light.ttf","ops");
        FontFactory.register(path_to_fonts+"MCTIME.TTF","russ");
        FontFactory.register(path_to_fonts+"FreeSerif.ttf","fser");
        FontFactory.register(path_to_fonts+"NotoSansSinhala-Regular.ttf","sinhala");
        FontFactory.register(path_to_fonts+"NotoSansBengali-Regular.ttf","bengali");
        FontFactory.register(path_to_fonts+"lohit_gu.ttf","punj");
        FontFactory.register(path_to_fonts+"FreeSans.ttf","fsans");
        FontFactory.register(path_to_fonts+"NotoSansTelugu-Regular.ttf","telugu");
        FontFactory.register(path_to_fonts+"Cybercjk.ttf","cjk");
        FontFactory.register(path_to_fonts+"IndUni-N-Roman.otf","ind");
        FontFactory.register(path_to_fonts+"lohit_or.ttf","oriya");
        FontFactory.register(path_to_fonts+"AppleColorEmoji.ttf","emojiAp");
        FontFactory.register(path_to_fonts+"android-emoji.ttf","emojiAn");
        FontFactory.register(path_to_fonts+"casy_ea.ttf","garif");



        int font_size = 6;
        Font cardo = FontFactory.getFont("cardo", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font roboto = FontFactory.getFont("roboto", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font russ = FontFactory.getFont("russ", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese1 = FontFactory.getFont("chinese1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese2 = FontFactory.getFont("chinese2", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese3 = FontFactory.getFont("chinese3", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese4 = FontFactory.getFont("chinese4", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese5 = FontFactory.getFont("chinese5", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese6 = FontFactory.getFont("chinese6", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab1 = FontFactory.getFont("arab1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab2 = FontFactory.getFont("arab2", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab3 = FontFactory.getFont("arab3", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font hebrew = FontFactory.getFont("hebrew", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font cherokee = FontFactory.getFont("cherokee", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font georgian = FontFactory.getFont("georgian", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font devanagari = FontFactory.getFont("devanagari", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font nanum = FontFactory.getFont("nanum", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font jap = FontFactory.getFont("jap", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font khmer = FontFactory.getFont("khmer", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font thai = FontFactory.getFont("thai", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font tamil = FontFactory.getFont("tamil", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font armenian = FontFactory.getFont("armenian", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font sinhala = FontFactory.getFont("sinhala", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font ops = FontFactory.getFont("ops", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font bengali = FontFactory.getFont("bengali", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font punj = FontFactory.getFont("punj", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fsans = FontFactory.getFont("fsans", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font dvs = FontFactory.getFont("dvs", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);                    
        Font telugu = FontFactory.getFont("telugu", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font cjk = FontFactory.getFont("cjk", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font ind = FontFactory.getFont("ind", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);                    
        Font oriya = FontFactory.getFont("oriya", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fser = FontFactory.getFont("fser", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fontGlyph = FontFactory.getFont("fontGlyph", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font emojiAn = FontFactory.getFont("emojiAn", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font emojiAp = FontFactory.getFont("emojiAp", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font garif = FontFactory.getFont("garif", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);

        pfs.addFont(cardo);
        pfs.addFont(fontGlyph);
        //fs.addFont(dvs);
        pfs.addFont(fser);
        pfs.addFont(cjk);
        pfs.addFont(russ);
        pfs.addFont(armenian);
        pfs.addFont(chinese1);
        pfs.addFont(chinese2);
        pfs.addFont(chinese3);
        pfs.addFont(chinese4);
        pfs.addFont(chinese5);
        pfs.addFont(chinese6);
        pfs.addFont(arab1);
        pfs.addFont(arab2);
        pfs.addFont(arab3);
//                    fs.addFont(ind);
        pfs.addFont(hebrew);
        pfs.addFont(cherokee);
        pfs.addFont(georgian);  
        pfs.addFont(devanagari);
        pfs.addFont(nanum);
        pfs.addFont(jap);
        pfs.addFont(khmer);
        pfs.addFont(thai);
        pfs.addFont(tamil);
        pfs.addFont(ops);
//        fs.addFont(helv);
        pfs.addFont(roboto);
        pfs.addFont(sinhala);
        pfs.addFont(bengali);
        pfs.addFont(punj);
        pfs.addFont(fsans);
        pfs.addFont(telugu);
        pfs.addFont(oriya);
        pfs.addFont(fser);
//        pfs.addFont(emojiAn);
//        pfs.addFont(emojiAp);
        fs.addFont(garif);
        
    }
            public void fontGet() throws DocumentException, IOException{
        String path_to_fonts = "/Users/wiki/repos/printwikipedia/dist/fonts/";
        FontFactory.register(path_to_fonts+"Cardo104s.ttf","cardo");
        FontFactory.register(path_to_fonts+"msgothic.tcc.0","fontGlyph");
        FontFactory.register(path_to_fonts+"cwTeXFangSong-zhonly.ttf","chinese1");
        FontFactory.register(path_to_fonts+"cwTeXHei-zhonly.ttf","chinese2");
        FontFactory.register(path_to_fonts+"cwTeXKai-zhonly.tff","chinese3");
        FontFactory.register(path_to_fonts+"cwTeXMing-zhonly.ttf","chinese4");
        FontFactory.register(path_to_fonts+"cwTeXYen-zhonly.ttf","chinese5");
        FontFactory.register(path_to_fonts+"G5LISL1B.TTF","chinese6");
        FontFactory.register(path_to_fonts+"Amiri-Regular.ttf","arab1");
        FontFactory.register(path_to_fonts+"DroidKufi-Regular.ttf","arab2");
        FontFactory.register(path_to_fonts+"Alef-Regular.ttf","hebrew");
        FontFactory.register(path_to_fonts+"NotoSansCherokee-Regular.ttf","cherokee");
        FontFactory.register(path_to_fonts+"NotoSansGeorgian-Regular.ttf","georgian");
        FontFactory.register(path_to_fonts+"NotoSansDevanagari-Regular.ttf","devanagari");
        FontFactory.register(path_to_fonts+"NanumGothic-Regular.ttf","nanum");
        FontFactory.register(path_to_fonts+"NotoKufiArabic-Regular.ttf","arab3");
        FontFactory.register(path_to_fonts+"NotoSansJP-Regular.otf","jap");
        FontFactory.register(path_to_fonts+"NotoSansKhmer-Regular.ttf","khmer");
        FontFactory.register(path_to_fonts+"NotoSansThai-Regular.ttf","thai");
        FontFactory.register(path_to_fonts+"NotoSerifArmenian-Regular.ttf","armenian");
        FontFactory.register(path_to_fonts+"NotoSansTamilUI-Regular.ttf","tamil");
        FontFactory.register(path_to_fonts+"DejaVuSans.ttf","dvs");
        FontFactory.register(path_to_fonts+"Roboto-Regular.ttf","roboto");
        FontFactory.register(path_to_fonts+"OpenSans-Light.ttf","ops");
        FontFactory.register(path_to_fonts+"MCTIME.TTF","russ");
        FontFactory.register(path_to_fonts+"FreeSerif.ttf","fser");
        FontFactory.register(path_to_fonts+"NotoSansSinhala-Regular.ttf","sinhala");
        FontFactory.register(path_to_fonts+"NotoSansBengali-Regular.ttf","bengali");
        FontFactory.register(path_to_fonts+"lohit_gu.ttf","punj");
        FontFactory.register(path_to_fonts+"FreeSans.ttf","fsans");
        FontFactory.register(path_to_fonts+"NotoSansTelugu-Regular.ttf","telugu");
        FontFactory.register(path_to_fonts+"Cybercjk.ttf","cjk");
        FontFactory.register(path_to_fonts+"IndUni-N-Roman.otf","ind");
        FontFactory.register(path_to_fonts+"lohit_or.ttf","oriya");
//      System.out.println(FontFactory.getRegisteredFonts().toString());
        FontFactory.register(path_to_fonts+"AppleColorEmoji.ttf","emojiAp");
        FontFactory.register(path_to_fonts+"android-emoji.ttf","emojiAn");
        FontFactory.register(path_to_fonts+"casy_ea.ttf","garif");


        int font_size = 8;
        
        
        BaseFont bsHelv = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        Font helv = new Font(bsHelv);
        Font cardo = FontFactory.getFont("cardo", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font roboto = FontFactory.getFont("roboto", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font russ = FontFactory.getFont("russ", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese1 = FontFactory.getFont("chinese1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese2 = FontFactory.getFont("chinese2", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese3 = FontFactory.getFont("chinese3", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese4 = FontFactory.getFont("chinese4", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese5 = FontFactory.getFont("chinese5", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font chinese6 = FontFactory.getFont("chinese6", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab1 = FontFactory.getFont("arab1", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab2 = FontFactory.getFont("arab2", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font arab3 = FontFactory.getFont("arab3", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font hebrew = FontFactory.getFont("hebrew", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font cherokee = FontFactory.getFont("cherokee", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font georgian = FontFactory.getFont("georgian", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font devanagari = FontFactory.getFont("devanagari", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font nanum = FontFactory.getFont("nanum", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font jap = FontFactory.getFont("jap", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font khmer = FontFactory.getFont("khmer", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font thai = FontFactory.getFont("thai", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font tamil = FontFactory.getFont("tamil", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font armenian = FontFactory.getFont("armenian", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font sinhala = FontFactory.getFont("sinhala", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font ops = FontFactory.getFont("ops", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font bengali = FontFactory.getFont("bengali", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font punj = FontFactory.getFont("punj", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fsans = FontFactory.getFont("fsans", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font dvs = FontFactory.getFont("dvs", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);                    
        Font telugu = FontFactory.getFont("telugu", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font cjk = FontFactory.getFont("cjk", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font ind = FontFactory.getFont("ind", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);                    
        Font oriya = FontFactory.getFont("oriya", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fser = FontFactory.getFont("fser", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font fontGlyph = FontFactory.getFont("fontGlyph", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font emojiAn = FontFactory.getFont("emojiAn", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font emojiAp = FontFactory.getFont("emojiAp", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        Font garif = FontFactory.getFont("garif", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, font_size);
        

        fs.addFont(cardo);
        fs.addFont(fontGlyph);
        //fs.addFont(dvs);
        fs.addFont(fser);
        fs.addFont(cjk);
        fs.addFont(russ);
        fs.addFont(armenian);
        fs.addFont(chinese1);
        fs.addFont(chinese2);
        fs.addFont(chinese3);
        fs.addFont(chinese4);
        fs.addFont(chinese5);
        fs.addFont(chinese6);
        fs.addFont(arab1);
        fs.addFont(arab2);
        fs.addFont(arab3);
//                    fs.addFont(ind);
        fs.addFont(hebrew);
        fs.addFont(cherokee);
        fs.addFont(georgian);
        fs.addFont(devanagari);
        fs.addFont(nanum);
        fs.addFont(jap);
        fs.addFont(khmer);
        fs.addFont(thai);
        fs.addFont(tamil);
        fs.addFont(ops);
        fs.addFont(helv);
        fs.addFont(roboto);
        fs.addFont(sinhala);
        fs.addFont(bengali);
        fs.addFont(punj);
        fs.addFont(fsans);
        fs.addFont(telugu);
        fs.addFont(oriya);
        fs.addFont(fser);
//        fs.addFont(emojiAn);
//        fs.addFont(emojiAp);
        fs.addFont(garif);
    }
    
    public void writePage(WikiPage page) {
        currentTitle = page.getTitle();
        currentArticleID = page.getId();
        String x = page.getRevision().getText().toLowerCase();
        if(x.contains("wiktionary redirect"))//breaks on wiktionary redirect. just ommit it. COULD REMOVE IF AFTER UPDATING wiki parse library!
            return;
        writeTitle(currentTitle);
        writeText(page.getRevision().getText());
    }


    //Write title of article to document
    private void writeTitle(String line) {
        Phrase ph = null;
        Paragraph pr = null;
        try {
            line = line.replaceAll("_", " ").toUpperCase();
            header.setCurrentTitle(line);
            ph = tfs.process(line);
            ph.setLeading(14);//changes leading between spaces in titles
            pr = new Paragraph(ph);
            pr.setSpacingBefore(8);//changes spacing before title
            pr.setSpacingAfter(4);//changes spacing after title.

            if (mct.isOverflow()) {
                mct.nextColumn();
                pdfDocument.newPage();
            }
            if (pdfWriter.getCurrentPageNumber() > 1) { //9/2015 not sure what these does. or why it's commented out...
                //Double paragraph helvetica problem is here other is in WikiHtmlConverter.java
//                mct.addElement(new Phrase("\n"));
            }

            mct.addElement(pr);
            pdfDocument.add(mct);
        } 
        catch (Exception ex) {
            WikiLogger.getLogger().severe(currentTitle + " - Error: " + ex.getMessage());
        }
    }

    //Write article text using defined styles
    private void writeText(String text) {
        try {
            //review the below removed/replaced items to see if you want to include. -- no good or easy way to add gallery though. probably don't want that.
            text = text.replaceAll("<gallery[\\s\\S]*?</gallery>",""); //no gallery

//             text is in BBCode (This is bliki)
            String html = WikiHtmlConverter.convertToHtml(text);
//            System.out.println(html);
            //these are being replaced both in the TOC of each entry and in the actual document. Easiest to remove here. kind of heavy on processor though...
            html = html.replaceAll("(?s)(<a id=\"See_also\" name=\"See_also\"></a><H2>SEE ALSO</H2>).*", "<b>_____________________</b><br /><br />");
            html = html.replaceAll("(?s)(<a id=\"References\" name=\"References\"></a><H2>REFERENCES</H2>).*", "<b>_____________________</b><br /><br />");
            html = html.replaceAll("(?s)(\\s+<a id=\"External_links\" name=\"External_links\"></a><H2>EXTERNAL LINKS</H2>).*","<b>_____________________</b><br /><br />");
            html = html.replaceAll("(?s)(\\s+<a id=\"Footnotes\" name=\"Footnotes\"></a><H2>FOOTNOTES</H2>).*","<b>_____________________</b><br /><br />");
            html = html.replaceAll("(?s)(\\s+<a id=\"Bibliography\" name=\"Bibliography\"></a><H2>BIBLIOGRAPHY</H2>).*","<b>_____________________</b><br /><br />");
            html = html.replaceAll("(?s)(\\s*<a\\s+id=\"Gallery\"\\s*name=\"Gallery\">\\s*</a>\\s*<H2>GALLERY</H2>).*","<b>_____________________</b><br /><br />");
            html = html.replaceAll("<li class=\"toclevel-1\"><a href=\"#Bibliography\">Bibliography</a>\n</li>", "");
            html = html.replaceAll("<li class=\"toclevel-1\"><a href=\"#Footnotes\">Footnotes</a>\n</li>", "");
            html = html.replaceAll("<li class=\"toclevel-1\"><a href=\"#References\">References</a>\n</li>", "");
            html = html.replaceAll("<li class=\"toclevel-1\"><a href=\"#External_links\">External links</a>\n</li>", "");
            html = html.replaceAll("<li class=\"toclevel-1\"><a href=\"#Gallery\">Gallery</a>\n</li>", "");
//            html = html.replaceAll("<a id=\"See_also\" name=\"See_also\"></a><H2>SEE ALSO</H2>", "");
            html = html.replaceAll("<li class=\"toclevel-1\"><a href=\"#See_also\">See also</a>\n</li>","");
            //html = html.replaceAll("(\\s+<a id) (?:(</a>))","");//removes anchor tags before H2 sections
            // text is now html (This is doing iText work)
            convertHtml2Pdf(html);
            // text has been made into pdf.
            
        } catch (Exception ex) {
            WikiLogger.getLogger().severe(currentTitle + " - Error: " + ex.getMessage());
        }
    }

    //Convert html to pdf objects, apply styles
    private void convertHtml2Pdf(String htmlSource) throws DocumentException, IOException {
        StringReader reader = new StringReader(htmlSource);
        StyleSheet styles = WikiStyles.getStyles();
        ArrayList objects;
        //parse that text!
        objects = WHTMLWorker.parseToList(reader, styles);
        
        for (int k = 0; k < objects.size(); ++k) {

            Element element = (Element) objects.get(k);
            //add objects
            if (mct.isOverflow()) {
                mct.nextColumn();
                pdfDocument.newPage();
            }
//this is where i first tried to add font stack and it worked well and fast but it could not parse everything correctly. only one font size was applied :\

//            
//            String temp_elem = element.toString();
//            temp_elem = temp_elem.substring(1, temp_elem.length()-1);
//            Phrase ph = fs.process(temp_elem);
            
//                ph.setLeading(8);
//                Paragraph pr = new Paragraph(ph);
//                System.out.println(pr.toString() + " this isthe paragraph");
                mct.addElement(element);
            pdfDocument.add(mct);
        }
    }

    

    /**
     *
     */
    public void closeColumn() {
        try {
            pdfDocument.add(mct);
        } catch (DocumentException ex) {
            WikiLogger.getLogger().severe(ex.getMessage());
            //throw new Exception(ex);
        }
    }

    /**
     *
     * @return
     */
    public String getOutputFileName() {
        return outputFileName;
    }

    /**
     *
     * @return
     */
    public int getPageNumb() {
        return pdfWriter.getCurrentPageNumber() - 1;
    }

    /**
     *
     * @return
     */
    public String getCurrentTitle(){
        return currentTitle;
    }

    /**
     *
     */
    public void close() throws DocumentException, IOException {
        System.out.println("i close!");
        
        pdfDocument.close();
        
    }
    public boolean checkOpen(){
        return pdfDocument.isOpen();
    }

    /**
     *
     * @return
     */
    public int getCurrentArticleID() {
        return currentArticleID;
    }

    private PageHeaderEvent header = null;
    private Document pdfDocument = null;
    private Document preDoc = null;
    private PdfWriter pdfWriter;
    private PdfWriter preWriter;
    private WikiFontSelector _wikiFontSelector = null;
    public static FontSelector fs = new FontSelector();
    private FontSelector tfs = new FontSelector();
    public static FontSelector pfs = new FontSelector();
    private MultiColumnText mct = null;
    private String outputFileName = "";
    private String prefn = "";
    private String currentTitle = "";
    private int currentArticleID;
    private BaseFont bflib;
}
