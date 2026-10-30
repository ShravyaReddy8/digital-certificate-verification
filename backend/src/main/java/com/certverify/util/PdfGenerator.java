package com.certverify.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders a professional, landscape A4 certificate PDF containing the course
 * details, a signature block and an embedded QR code that links to the public
 * verification page.
 */
public final class PdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();   // 841.89
    private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();   // 595.28

    private static final int[] NAVY = {15, 42, 84};
    private static final int[] BLUE = {37, 99, 235};
    private static final int[] GOLD = {191, 155, 48};
    private static final int[] GRAY = {95, 105, 120};
    private static final int[] DARK = {24, 32, 45};

    private PdfGenerator() {
    }

    public static byte[] generateCertificatePdf(String institutionName,
                                                String studentName,
                                                String courseTitle,
                                                String grade,
                                                LocalDate issueDate,
                                                LocalDate expiryDate,
                                                String certificateId,
                                                String issuerName,
                                                byte[] qrPng,
                                                String verifyUrl) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);

            PDFont bold = PDType1Font.HELVETICA_BOLD;
            PDFont regular = PDType1Font.HELVETICA;
            PDFont italic = PDType1Font.HELVETICA_OBLIQUE;

            String institution = ascii(institutionName, 64);
            String student = ascii(studentName, 46);
            String course = ascii(courseTitle, 70);
            String gradeText = ascii(grade == null || grade.isBlank() ? "Completed" : grade, 30);
            String issuer = ascii(issuerName, 46);
            String url = ascii(verifyUrl, 120);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // Outer double border
                drawBorder(content, 24f, 3.2f, NAVY);
                drawBorder(content, 33f, 1f, GOLD);

                // Header band
                content.setNonStrokingColor(NAVY[0], NAVY[1], NAVY[2]);
                content.addRect(0, PAGE_HEIGHT - 48, PAGE_WIDTH, 48);
                content.fill();
                drawCentered(content, bold, 15, institution.toUpperCase(Locale.ENGLISH),
                        PAGE_WIDTH, PAGE_HEIGHT - 31, 255, 255, 255);

                // Title
                drawCentered(content, bold, 31, "CERTIFICATE OF COMPLETION", PAGE_WIDTH, 492, NAVY[0], NAVY[1], NAVY[2]);
                content.setNonStrokingColor(GOLD[0], GOLD[1], GOLD[2]);
                content.addRect(PAGE_WIDTH / 2 - 70, 476, 140, 3);
                content.fill();

                drawCentered(content, italic, 12.5f, "This is to certify that", PAGE_WIDTH, 444, GRAY[0], GRAY[1], GRAY[2]);

                // Student name + underline
                drawCentered(content, bold, 28, student, PAGE_WIDTH, 398, DARK[0], DARK[1], DARK[2]);
                content.setStrokingColor(GOLD[0], GOLD[1], GOLD[2]);
                content.setLineWidth(1.2f);
                content.moveTo(PAGE_WIDTH / 2 - 215, 384);
                content.lineTo(PAGE_WIDTH / 2 + 215, 384);
                content.stroke();

                drawCentered(content, regular, 11.5f, "has successfully completed the course",
                        PAGE_WIDTH, 356, GRAY[0], GRAY[1], GRAY[2]);

                // Course title, auto-shrinking if required
                float courseSize = 21f;
                while (courseSize > 12f && widthOf(bold, courseSize, course) > 620) {
                    courseSize -= 0.5f;
                }
                drawCentered(content, bold, courseSize, course, PAGE_WIDTH, 323, BLUE[0], BLUE[1], BLUE[2]);

                String details = "Issued on " + DATE_FORMAT.format(issueDate)
                        + "   |   Certificate ID " + certificateId;
                drawCentered(content, regular, 10, details, PAGE_WIDTH, 292, GRAY[0], GRAY[1], GRAY[2]);

                // Grade and validity block
                drawLabelValue(content, 70, 240, "GRADE / REMARKS", gradeText, bold, regular);
                String validUntil = expiryDate == null ? "No expiry" : DATE_FORMAT.format(expiryDate);
                drawLabelValue(content, 250, 240, "VALID UNTIL", validUntil, bold, regular);

                // Signature block
                content.setStrokingColor(DARK[0], DARK[1], DARK[2]);
                content.setLineWidth(0.8f);
                content.moveTo(70, 158);
                content.lineTo(270, 158);
                content.stroke();
                content.setNonStrokingColor(DARK[0], DARK[1], DARK[2]);
                drawText(content, bold, 11, issuer, 70, 142);
                drawText(content, regular, 8.5f, "Authorized Signatory", 70, 128);
                drawText(content, regular, 8.5f, institution, 70, 116);

                // QR code
                if (qrPng != null && qrPng.length > 0) {
                    PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "verification-qr");
                    content.drawImage(qrImage, PAGE_WIDTH - 170, 78, 104, 104);
                    drawCenteredAt(content, regular, 8, "Scan to verify", PAGE_WIDTH - 118, 66, GRAY[0], GRAY[1], GRAY[2]);
                }

                // Footer
                String footer = "This certificate is digitally signed and tamper-evident. Verify at " + url;
                float footerSize = 7.5f;
                while (footerSize > 5.5f && widthOf(regular, footerSize, footer) > 700) {
                    footerSize -= 0.25f;
                }
                drawCentered(content, regular, footerSize, footer, PAGE_WIDTH, 48, GRAY[0], GRAY[1], GRAY[2]);
            }

            document.getDocumentInformation().setTitle("Certificate " + certificateId);
            document.getDocumentInformation().setAuthor(institution);
            document.getDocumentInformation().setSubject("Certificate of Completion - " + course);
            document.getDocumentInformation().setCreator("Digital Certificate Verification System");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static void drawBorder(PDPageContentStream content, float inset, float lineWidth, int[] color)
            throws IOException {
        content.setStrokingColor(color[0], color[1], color[2]);
        content.setLineWidth(lineWidth);
        content.addRect(inset, inset, PAGE_WIDTH - inset * 2, PAGE_HEIGHT - inset * 2);
        content.stroke();
    }

    private static void drawLabelValue(PDPageContentStream content, float x, float y, String label,
                                       String value, PDFont bold, PDFont regular) throws IOException {
        content.setNonStrokingColor(GRAY[0], GRAY[1], GRAY[2]);
        drawText(content, regular, 7.5f, label, x, y + 16);
        content.setNonStrokingColor(NAVY[0], NAVY[1], NAVY[2]);
        float size = 13f;
        while (size > 8f && widthOf(bold, size, value) > 170) {
            size -= 0.5f;
        }
        drawText(content, bold, size, value, x, y);
    }

    private static void drawCentered(PDPageContentStream content, PDFont font, float size, String text,
                                     float pageWidth, float y, int r, int g, int b) throws IOException {
        drawCenteredAt(content, font, size, text, pageWidth / 2, y, r, g, b);
    }

    private static void drawCenteredAt(PDPageContentStream content, PDFont font, float size, String text,
                                       float centerX, float y, int r, int g, int b) throws IOException {
        float textWidth = widthOf(font, size, text);
        content.setNonStrokingColor(r, g, b);
        drawText(content, font, size, text, centerX - textWidth / 2, y);
    }

    private static void drawText(PDPageContentStream content, PDFont font, float size, String text,
                                 float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private static float widthOf(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    /**
     * The PDF uses the standard (WinAnsi) fonts, so any character outside that
     * set is transliterated before drawing.
     */
    private static String ascii(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value
                .replace("\u2014", "-").replace("\u2013", "-")
                .replace("\u2018", "'").replace("\u2019", "'")
                .replace("\u201C", "\"").replace("\u201D", "\"")
                .replace("\u2022", "-").replace("\u00A0", " ")
                .replace("\n", " ").replace("\r", " ");
        StringBuilder sb = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (c == '\u00A9') {
                sb.append("(c)");
            }
        }
        String result = sb.toString().trim().replaceAll("\\s{2,}", " ");
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength - 3).trim() + "...";
        }
        return result;
    }
}
