package com.personalai.personal_ai.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeTextExtractor {

    public String extract(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String extension = filename == null ? "" : filename
                .substring(Math.max(filename.lastIndexOf('.'), 0))
                .toLowerCase(Locale.ROOT);

        return switch (extension) {
            case ".pdf" -> extractPdf(file.getInputStream());
            case ".docx" -> extractDocx(file.getInputStream());
            default -> throw new IllegalArgumentException(
                    "Only PDF and DOCX files are supported.");
        };
    }

    private String extractPdf(InputStream input) throws IOException {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(InputStream input) throws IOException {
        try (XWPFDocument document = new XWPFDocument(input)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(text, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                table.getRows().forEach(row -> row.getTableCells().forEach(cell ->
                        appendLine(text, cell.getText())));
            }
            return text.toString();
        }
    }

    private void appendLine(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            text.append(value.trim()).append('\n');
        }
    }
}
