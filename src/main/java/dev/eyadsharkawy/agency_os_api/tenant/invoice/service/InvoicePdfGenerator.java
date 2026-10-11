package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class InvoicePdfGenerator {

  private InvoicePdfGenerator() {
    // Utility class
  }

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static byte[] generate(
      Invoice invoice, String agencyName, String contactEmail, List<TimeEntry> billedEntries)
      throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);

      // Open the content stream for the first page
      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      PDFont titleFont;
      PDFont standardFont;
      PDFont standardBoldFont;

      try (InputStream regStream =
              InvoicePdfGenerator.class.getResourceAsStream("/fonts/Amiri-Regular.ttf");
          InputStream boldStream =
              InvoicePdfGenerator.class.getResourceAsStream("/fonts/Amiri-Bold.ttf")) {
        if (regStream != null && boldStream != null) {
          standardFont = PDType0Font.load(document, regStream);
          standardBoldFont = PDType0Font.load(document, boldStream);
          titleFont = standardBoldFont;
        } else {
          titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
          standardFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
          standardBoldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        }
      }

      // --- BRAND COLORS ---
      float brandR = 55 / 255f;
      float brandG = 120 / 255f;
      float brandB = 195 / 255f;

      // Draw top colored band on page 1
      contentStream.setNonStrokingColor(brandR, brandG, brandB);
      contentStream.addRect(0, 770, 612, 22);
      contentStream.fill();

      // --- FROM SECTION (SENDER) ---
      contentStream.beginText();
      contentStream.setFont(titleFont, 16);
      contentStream.setNonStrokingColor(0, 0, 0); // Black text
      contentStream.newLineAtOffset(50, 725);
      contentStream.showText(safeText(agencyName, titleFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardFont, 10);
      contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f); // Gray text
      contentStream.newLineAtOffset(50, 708);
      contentStream.showText(safeText("Contact: " + contactEmail, standardFont));
      contentStream.endText();

      // --- BILLING INFO GRID ---
      float gridLabelY = 660f;
      float gridValueY = 645f;

      // Column 1: BILL TO
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(50, gridLabelY);
      contentStream.showText(safeText("BILL TO", standardBoldFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 10);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(50, gridValueY);
      contentStream.showText(safeText(invoice.getClient().getName(), standardBoldFont));
      contentStream.newLineAtOffset(0, -13);
      contentStream.setFont(standardFont, 9);
      contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
      contentStream.showText(
          safeText(
              invoice.getClient().getEmail() != null ? invoice.getClient().getEmail() : "N/A",
              standardFont));
      contentStream.endText();

      // Column 2: PROJECT
      String projectName = "General Billing";
      if (billedEntries != null && !billedEntries.isEmpty()) {
        long uniqueProjects =
            billedEntries.stream()
                .map(entry -> entry.getTask().getProject().getId())
                .distinct()
                .count();

        if (uniqueProjects == 1) {
          projectName = billedEntries.get(0).getTask().getProject().getName();
        } else if (uniqueProjects > 1) {
          projectName = "Multiple Projects";
        }
      }
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(210, gridLabelY);
      contentStream.showText(safeText("PROJECT", standardBoldFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardFont, 10);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(210, gridValueY);
      contentStream.showText(safeText(projectName, standardFont));
      contentStream.endText();

      // Column 3: INVOICE DETAILS
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(350, gridLabelY);
      contentStream.showText(safeText("INVOICE #", standardBoldFont));
      contentStream.newLineAtOffset(0, -15);
      contentStream.showText(safeText("DATE", standardBoldFont));
      contentStream.newLineAtOffset(0, -15);
      contentStream.showText(safeText("DUE DATE", standardBoldFont));
      contentStream.endText();

      Instant dueDate = invoice.getCreatedAt().plus(Duration.ofDays(30)); // Net 30

      // Render full UUID at size 8
      contentStream.beginText();
      contentStream.setFont(standardFont, 8);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(425, gridLabelY);
      contentStream.showText(safeText(invoice.getId().toString(), standardFont));
      contentStream.endText();

      // Render Dates at size 9
      contentStream.beginText();
      contentStream.setFont(standardFont, 9);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(425, gridLabelY - 15f);
      contentStream.showText(
          safeText(
              invoice.getCreatedAt().atZone(ZoneOffset.UTC).format(DATE_FORMATTER).substring(0, 10),
              standardFont));
      contentStream.newLineAtOffset(0, -15);
      contentStream.showText(
          safeText(
              dueDate.atZone(ZoneOffset.UTC).format(DATE_FORMATTER).substring(0, 10),
              standardFont));
      contentStream.endText();

      // --- INVOICE TOTAL HIGHLIGHT BLOCK ---
      contentStream.setLineWidth(1f);
      contentStream.setStrokingColor(220 / 255f, 220 / 255f, 220 / 255f);
      contentStream.moveTo(50, 580);
      contentStream.lineTo(562, 580);
      contentStream.stroke();

      contentStream.moveTo(50, 530);
      contentStream.lineTo(562, 530);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(titleFont, 20);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(50, 548);
      contentStream.showText(safeText("Invoice Total", titleFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(titleFont, 22);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(450, 548);
      contentStream.showText(
          safeText("$" + invoice.getTotalAmount().setScale(2).toString(), titleFont));
      contentStream.endText();

      // --- TABLE HEADERS ---
      int tableHeaderY = 495;
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(50, tableHeaderY);
      contentStream.showText(safeText("ACTIVITY", standardBoldFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(390, tableHeaderY);
      contentStream.showText(safeText("RATE", standardBoldFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(480, tableHeaderY);
      contentStream.showText(safeText("AMOUNT", standardBoldFont));
      contentStream.endText();

      contentStream.setLineWidth(0.8f);
      contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
      contentStream.moveTo(50, 485);
      contentStream.lineTo(562, 485);
      contentStream.stroke();

      // --- DRAW TABLE ITEMS (WITH MULTI-PAGE FLOW & WRAPPING) ---
      float yPosition = 465f;
      float activityMaxWidth = 330f;
      float lineSpacing = 13f;

      for (TimeEntry entry : billedEntries) {
        BigDecimal hours =
            BigDecimal.valueOf(entry.getDurationMinutes())
                .divide(BigDecimal.valueOf(60.0), 4, RoundingMode.HALF_UP);
        BigDecimal billingRate =
            entry.getTask().getProject().getBillingRate() != null
                ? entry.getTask().getProject().getBillingRate()
                : BigDecimal.ZERO;
        BigDecimal entryCost = hours.multiply(billingRate).setScale(2, RoundingMode.HALF_UP);

        String projectNameVal = entry.getTask().getProject().getName();
        String taskTitleVal = entry.getTask().getTitle();
        String itemDesc =
            String.format(
                "%s - %s (%s)",
                projectNameVal, taskTitleVal, formatDuration(entry.getDurationMinutes()));

        List<String> descLines = wrapText(itemDesc, standardFont, 10, activityMaxWidth);
        if (descLines.isEmpty()) {
          descLines = List.of(safeText(itemDesc, standardFont));
        }

        float rowHeight = Math.max(20f, (descLines.size() * lineSpacing) + 6f);

        // Page break check: if row would overflow below bottom margin (Y < 80)
        if (yPosition - rowHeight < 80) {
          contentStream.close(); // Close current page stream

          page = new PDPage();
          document.addPage(page);
          contentStream = new PDPageContentStream(document, page);

          // Draw top colored band on page 2+
          contentStream.setNonStrokingColor(brandR, brandG, brandB);
          contentStream.addRect(0, 770, 612, 22);
          contentStream.fill();

          // Re-draw Table Headers on the new page
          float newPageHeaderY = 730f;
          contentStream.beginText();
          contentStream.setFont(standardBoldFont, 9);
          contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
          contentStream.newLineAtOffset(50, newPageHeaderY);
          contentStream.showText(safeText("ACTIVITY", standardBoldFont));
          contentStream.endText();

          contentStream.beginText();
          contentStream.setFont(standardBoldFont, 9);
          contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
          contentStream.newLineAtOffset(390, newPageHeaderY);
          contentStream.showText(safeText("RATE", standardBoldFont));
          contentStream.endText();

          contentStream.beginText();
          contentStream.setFont(standardBoldFont, 9);
          contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
          contentStream.newLineAtOffset(480, newPageHeaderY);
          contentStream.showText(safeText("AMOUNT", standardBoldFont));
          contentStream.endText();

          // Draw separator line under headers on new page
          contentStream.setLineWidth(0.8f);
          contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
          contentStream.moveTo(50, newPageHeaderY - 10f);
          contentStream.lineTo(562, newPageHeaderY - 10f);
          contentStream.stroke();

          yPosition = newPageHeaderY - 30f; // Reset Y drawing coordinate
        }

        // DESCRIPTION (Multiple wrapped lines)
        float textY = yPosition;
        for (String line : descLines) {
          contentStream.beginText();
          contentStream.setFont(standardFont, 10);
          contentStream.setNonStrokingColor(0, 0, 0);
          contentStream.newLineAtOffset(50, textY);
          contentStream.showText(line);
          contentStream.endText();
          textY -= lineSpacing;
        }

        // UNIT PRICE (Rate)
        contentStream.beginText();
        contentStream.setFont(standardFont, 10);
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.newLineAtOffset(390, yPosition);
        contentStream.showText(safeText("$" + billingRate.setScale(2).toString(), standardFont));
        contentStream.endText();

        // AMOUNT (Cost)
        contentStream.beginText();
        contentStream.setFont(standardFont, 10);
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.newLineAtOffset(480, yPosition);
        contentStream.showText(safeText("$" + entryCost.setScale(2).toString(), standardFont));
        contentStream.endText();

        yPosition -= rowHeight;
      }

      // Table bottom separator line
      contentStream.setLineWidth(0.8f);
      contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
      contentStream.moveTo(50, yPosition + 4f);
      contentStream.lineTo(562, yPosition + 4f);
      contentStream.stroke();

      // --- 7. TOTAL DUE & TERMS PAGE BREAK PROTECTION ---
      // If there's not enough room left on the current page to fit the totals and terms (requires
      // ~130pt)
      if (yPosition < 130) {
        contentStream.close();

        page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);

        // Draw top colored band on final overflow page
        contentStream.setNonStrokingColor(brandR, brandG, brandB);
        contentStream.addRect(0, 770, 612, 22);
        contentStream.fill();

        yPosition = 740; // Reset Y position to top
      }

      // Draw Total Due
      float totalY = yPosition - 5f;
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 11);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(350, totalY);
      contentStream.showText(safeText("Total Due", standardBoldFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 11);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(480, totalY);
      contentStream.showText(
          safeText("$" + invoice.getTotalAmount().setScale(2).toString(), standardBoldFont));
      contentStream.endText();

      // --- 8. TERMS & CONDITIONS ---
      float termsY = totalY - 50f;
      // If terms get pushed too close to the bottom band, float them at Y = 85
      if (termsY < 85) {
        termsY = 85f;
      }
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 10);
      contentStream.setNonStrokingColor(brandR, brandG, brandB);
      contentStream.newLineAtOffset(50, termsY);
      contentStream.showText(safeText("TERMS & CONDITIONS", standardBoldFont));
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardFont, 9);
      contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
      contentStream.newLineAtOffset(50, termsY - 16f);
      contentStream.showText(safeText("Payment is due within 30 days of issue.", standardFont));
      contentStream.endText();

      // --- 9. BOTTOM BRAND BAND ---
      contentStream.setNonStrokingColor(brandR, brandG, brandB);
      contentStream.addRect(0, 0, 612, 22); // Bottom colored band
      contentStream.fill();

      // Close the final content stream
      contentStream.close();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      document.save(baos);
      return baos.toByteArray();
    }
  }

  static String formatDuration(int minutes) {
    if (minutes <= 0) {
      return "0 mins";
    }
    if (minutes < 60) {
      return minutes + (minutes == 1 ? " min" : " mins");
    }
    int hours = minutes / 60;
    int remMinutes = minutes % 60;
    if (remMinutes == 0) {
      return hours + (hours == 1 ? " hr" : " hrs");
    }
    return String.format(
        "%d hr%s %d min%s", hours, hours > 1 ? "s" : "", remMinutes, remMinutes > 1 ? "s" : "");
  }

  static String safeText(String text, PDFont font) {
    if (text == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); ) {
      int codePoint = text.codePointAt(i);
      i += Character.charCount(codePoint);
      if (codePoint == '\r' || codePoint == '\n' || Character.isISOControl(codePoint)) {
        sb.append(' ');
        continue;
      }
      String charStr = new String(Character.toChars(codePoint));
      try {
        font.encode(charStr);
        sb.append(charStr);
      } catch (Exception e) {
        sb.append('?');
      }
    }
    return sb.toString();
  }

  static float getTextWidth(String text, PDFont font, float fontSize) {
    try {
      return font.getStringWidth(text) / 1000f * fontSize;
    } catch (IOException e) {
      return text.length() * fontSize * 0.5f;
    }
  }

  static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) {
    List<String> lines = new ArrayList<>();
    String safe = safeText(text, font);
    if (safe.isBlank()) {
      return lines;
    }

    String[] words = safe.split("\\s+");
    StringBuilder currentLine = new StringBuilder();

    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
      float lineWidth = getTextWidth(testLine, font, fontSize);

      if (lineWidth <= maxWidth) {
        currentLine = new StringBuilder(testLine);
      } else {
        if (!currentLine.isEmpty()) {
          lines.add(currentLine.toString());
          currentLine = new StringBuilder();
        }
        if (getTextWidth(word, font, fontSize) > maxWidth) {
          StringBuilder piece = new StringBuilder();
          for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (getTextWidth(piece.toString() + c, font, fontSize) <= maxWidth) {
              piece.append(c);
            } else {
              if (!piece.isEmpty()) {
                lines.add(piece.toString());
                piece = new StringBuilder();
              }
              piece.append(c);
            }
          }
          currentLine = piece;
        } else {
          currentLine = new StringBuilder(word);
        }
      }
    }

    if (!currentLine.isEmpty()) {
      lines.add(currentLine.toString());
    }

    return lines;
  }
}
