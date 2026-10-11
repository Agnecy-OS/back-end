package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
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

  private static final float BRAND_R = 55 / 255f;
  private static final float BRAND_G = 120 / 255f;
  private static final float BRAND_B = 195 / 255f;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private record InvoiceFonts(PDFont standard, PDFont bold, PDFont title) {}

  private InvoicePdfGenerator() {
    // Utility class
  }

  public static byte[] generate(
      Invoice invoice, String agencyName, String contactEmail, List<TimeEntry> billedEntries)
      throws IOException {
    try (PDDocument document = new PDDocument()) {
      InvoiceFonts fonts = loadFonts(document);

      PDPage page = new PDPage();
      document.addPage(page);
      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      drawTopBrandBand(contentStream);
      drawSenderInfo(contentStream, fonts, agencyName, contactEmail);

      String projectName = determineProjectName(billedEntries);
      drawBillingInfoGrid(contentStream, fonts, invoice, projectName);
      drawInvoiceTotalHighlight(contentStream, fonts, invoice.getTotalAmount());
      drawTableHeaders(contentStream, fonts, 495f);

      contentStream = renderTableAndFooter(document, contentStream, fonts, invoice, billedEntries);
      contentStream.close();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      document.save(baos);
      return baos.toByteArray();
    }
  }

  private static InvoiceFonts loadFonts(PDDocument document) throws IOException {
    try (InputStream regStream =
            InvoicePdfGenerator.class.getResourceAsStream("/fonts/Amiri-Regular.ttf");
        InputStream boldStream =
            InvoicePdfGenerator.class.getResourceAsStream("/fonts/Amiri-Bold.ttf")) {
      if (regStream != null && boldStream != null) {
        PDFont regular = PDType0Font.load(document, regStream);
        PDFont bold = PDType0Font.load(document, boldStream);
        return new InvoiceFonts(regular, bold, bold);
      }
    }
    PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    return new InvoiceFonts(regular, bold, bold);
  }

  private static String determineProjectName(List<TimeEntry> billedEntries) {
    if (billedEntries == null || billedEntries.isEmpty()) {
      return "General Billing";
    }
    long uniqueProjects =
        billedEntries.stream()
            .map(entry -> entry.getTask().getProject().getId())
            .distinct()
            .count();
    if (uniqueProjects == 1) {
      return billedEntries.get(0).getTask().getProject().getName();
    } else if (uniqueProjects > 1) {
      return "Multiple Projects";
    }
    return "General Billing";
  }

  private static void drawTopBrandBand(PDPageContentStream contentStream) throws IOException {
    contentStream.setNonStrokingColor(BRAND_R, BRAND_G, BRAND_B);
    contentStream.addRect(0, 770, 612, 22);
    contentStream.fill();
  }

  private static void drawSenderInfo(
      PDPageContentStream contentStream, InvoiceFonts fonts, String agencyName, String contactEmail)
      throws IOException {
    contentStream.beginText();
    contentStream.setFont(fonts.title(), 16);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(50, 725);
    contentStream.showText(safeText(agencyName, fonts.title()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 10);
    contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
    contentStream.newLineAtOffset(50, 708);
    contentStream.showText(safeText("Contact: " + contactEmail, fonts.standard()));
    contentStream.endText();
  }

  private static void drawBillingInfoGrid(
      PDPageContentStream contentStream, InvoiceFonts fonts, Invoice invoice, String projectName)
      throws IOException {
    float gridLabelY = 660f;
    float gridValueY = 645f;

    // Column 1: BILL TO
    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 9);
    contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
    contentStream.newLineAtOffset(50, gridLabelY);
    contentStream.showText(safeText("BILL TO", fonts.bold()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 10);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(50, gridValueY);
    contentStream.showText(safeText(invoice.getClient().getName(), fonts.bold()));
    contentStream.newLineAtOffset(0, -13);
    contentStream.setFont(fonts.standard(), 9);
    contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
    String email = invoice.getClient().getEmail() != null ? invoice.getClient().getEmail() : "N/A";
    contentStream.showText(safeText(email, fonts.standard()));
    contentStream.endText();

    // Column 2: PROJECT
    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 9);
    contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
    contentStream.newLineAtOffset(210, gridLabelY);
    contentStream.showText(safeText("PROJECT", fonts.bold()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 10);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(210, gridValueY);
    contentStream.showText(safeText(projectName, fonts.standard()));
    contentStream.endText();

    // Column 3: INVOICE DETAILS
    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 9);
    contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
    contentStream.newLineAtOffset(350, gridLabelY);
    contentStream.showText(safeText("INVOICE #", fonts.bold()));
    contentStream.newLineAtOffset(0, -15);
    contentStream.showText(safeText("DATE", fonts.bold()));
    contentStream.newLineAtOffset(0, -15);
    contentStream.showText(safeText("DUE DATE", fonts.bold()));
    contentStream.endText();

    Instant dueDate = invoice.getCreatedAt().plus(Duration.ofDays(30));

    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 8);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(425, gridLabelY);
    contentStream.showText(safeText(invoice.getId().toString(), fonts.standard()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 9);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(425, gridLabelY - 15f);
    contentStream.showText(
        safeText(
            invoice.getCreatedAt().atZone(ZoneOffset.UTC).format(DATE_FORMATTER).substring(0, 10),
            fonts.standard()));
    contentStream.newLineAtOffset(0, -15);
    contentStream.showText(
        safeText(
            dueDate.atZone(ZoneOffset.UTC).format(DATE_FORMATTER).substring(0, 10),
            fonts.standard()));
    contentStream.endText();
  }

  private static void drawInvoiceTotalHighlight(
      PDPageContentStream contentStream, InvoiceFonts fonts, BigDecimal totalAmount)
      throws IOException {
    contentStream.setLineWidth(1f);
    contentStream.setStrokingColor(220 / 255f, 220 / 255f, 220 / 255f);
    contentStream.moveTo(50, 580);
    contentStream.lineTo(562, 580);
    contentStream.stroke();

    contentStream.moveTo(50, 530);
    contentStream.lineTo(562, 530);
    contentStream.stroke();

    contentStream.beginText();
    contentStream.setFont(fonts.title(), 20);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(50, 548);
    contentStream.showText(safeText("Invoice Total", fonts.title()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.title(), 22);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(450, 548);
    contentStream.showText(safeText("$" + totalAmount.setScale(2).toString(), fonts.title()));
    contentStream.endText();
  }

  private static void drawTableHeaders(
      PDPageContentStream contentStream, InvoiceFonts fonts, float headerY) throws IOException {
    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 9);
    contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
    contentStream.newLineAtOffset(50, headerY);
    contentStream.showText(safeText("ACTIVITY", fonts.bold()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 9);
    contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
    contentStream.newLineAtOffset(390, headerY);
    contentStream.showText(safeText("RATE", fonts.bold()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 9);
    contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
    contentStream.newLineAtOffset(480, headerY);
    contentStream.showText(safeText("AMOUNT", fonts.bold()));
    contentStream.endText();

    contentStream.setLineWidth(0.8f);
    contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
    contentStream.moveTo(50, headerY - 10f);
    contentStream.lineTo(562, headerY - 10f);
    contentStream.stroke();
  }

  private static PDPageContentStream renderTableAndFooter(
      PDDocument document,
      PDPageContentStream contentStream,
      InvoiceFonts fonts,
      Invoice invoice,
      List<TimeEntry> billedEntries)
      throws IOException {
    float yPosition = 465f;
    float activityMaxWidth = 330f;
    float lineSpacing = 13f;
    PDPageContentStream[] streamHolder = new PDPageContentStream[] {contentStream};

    if (billedEntries == null || billedEntries.isEmpty()) {
      String fallbackDesc = "General Billing - Services Rendered";
      List<String> descLines = wrapText(fallbackDesc, fonts.standard(), 10, activityMaxWidth);
      drawTableRow(
          streamHolder[0],
          fonts,
          descLines,
          null,
          invoice.getTotalAmount(),
          yPosition,
          lineSpacing);
      yPosition -= Math.max(20f, (descLines.size() * lineSpacing) + 6f);
    } else {
      for (TimeEntry entry : billedEntries) {
        yPosition =
            renderEntryRow(
                document, streamHolder, fonts, entry, yPosition, lineSpacing, activityMaxWidth);
      }
    }

    contentStream = streamHolder[0];
    contentStream.setLineWidth(0.8f);
    contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
    contentStream.moveTo(50, yPosition + 4f);
    contentStream.lineTo(562, yPosition + 4f);
    contentStream.stroke();

    if (yPosition < 130) {
      contentStream.close();
      PDPage finalPage = new PDPage();
      document.addPage(finalPage);
      contentStream = new PDPageContentStream(document, finalPage);
      drawTopBrandBand(contentStream);
      yPosition = 740f;
    }

    drawTermsAndFooter(contentStream, fonts, invoice.getTotalAmount(), yPosition);
    return contentStream;
  }

  private static float renderEntryRow(
      PDDocument document,
      PDPageContentStream[] streamHolder,
      InvoiceFonts fonts,
      TimeEntry entry,
      float yPosition,
      float lineSpacing,
      float activityMaxWidth)
      throws IOException {
    BigDecimal hours =
        BigDecimal.valueOf(entry.getDurationMinutes())
            .divide(BigDecimal.valueOf(60.0), 4, RoundingMode.HALF_UP);
    BigDecimal billingRate =
        (entry.getTask() != null
                && entry.getTask().getProject() != null
                && entry.getTask().getProject().getBillingRate() != null)
            ? entry.getTask().getProject().getBillingRate()
            : BigDecimal.ZERO;
    BigDecimal entryCost = hours.multiply(billingRate).setScale(2, RoundingMode.HALF_UP);

    String projectNameVal =
        (entry.getTask() != null && entry.getTask().getProject() != null)
            ? entry.getTask().getProject().getName()
            : "General";
    String taskTitleVal =
        (entry.getTask() != null && entry.getTask().getTitle() != null)
            ? entry.getTask().getTitle()
            : "Task";
    String itemDesc =
        String.format(
            "%s - %s (%s)",
            projectNameVal, taskTitleVal, formatDuration(entry.getDurationMinutes()));

    List<String> descLines = wrapText(itemDesc, fonts.standard(), 10, activityMaxWidth);
    if (descLines.isEmpty()) {
      descLines = List.of(safeText(itemDesc, fonts.standard()));
    }

    float rowHeight = Math.max(20f, (descLines.size() * lineSpacing) + 6f);
    if (yPosition - rowHeight < 80) {
      streamHolder[0].close();
      PDPage newPage = new PDPage();
      document.addPage(newPage);
      streamHolder[0] = new PDPageContentStream(document, newPage);
      drawTopBrandBand(streamHolder[0]);
      drawTableHeaders(streamHolder[0], fonts, 730f);
      yPosition = 700f;
    }

    drawTableRow(streamHolder[0], fonts, descLines, billingRate, entryCost, yPosition, lineSpacing);
    return yPosition - rowHeight;
  }

  private static void drawTableRow(
      PDPageContentStream contentStream,
      InvoiceFonts fonts,
      List<String> descLines,
      BigDecimal billingRate,
      BigDecimal entryCost,
      float yPosition,
      float lineSpacing)
      throws IOException {
    float textY = yPosition;
    for (String line : descLines) {
      contentStream.beginText();
      contentStream.setFont(fonts.standard(), 10);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(50, textY);
      contentStream.showText(line);
      contentStream.endText();
      textY -= lineSpacing;
    }

    String rateStr = billingRate != null ? "$" + billingRate.setScale(2).toString() : "-";
    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 10);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(390, yPosition);
    contentStream.showText(safeText(rateStr, fonts.standard()));
    contentStream.endText();

    String amountStr = entryCost != null ? "$" + entryCost.setScale(2).toString() : "$0.00";
    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 10);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(480, yPosition);
    contentStream.showText(safeText(amountStr, fonts.standard()));
    contentStream.endText();
  }

  private static void drawTermsAndFooter(
      PDPageContentStream contentStream,
      InvoiceFonts fonts,
      BigDecimal totalAmount,
      float yPosition)
      throws IOException {
    float totalY = yPosition - 5f;
    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 11);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(350, totalY);
    contentStream.showText(safeText("Total Due", fonts.bold()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 11);
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.newLineAtOffset(480, totalY);
    contentStream.showText(safeText("$" + totalAmount.setScale(2).toString(), fonts.bold()));
    contentStream.endText();

    float termsY = Math.max(85f, totalY - 50f);
    contentStream.beginText();
    contentStream.setFont(fonts.bold(), 10);
    contentStream.setNonStrokingColor(BRAND_R, BRAND_G, BRAND_B);
    contentStream.newLineAtOffset(50, termsY);
    contentStream.showText(safeText("TERMS & CONDITIONS", fonts.bold()));
    contentStream.endText();

    contentStream.beginText();
    contentStream.setFont(fonts.standard(), 9);
    contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
    contentStream.newLineAtOffset(50, termsY - 16f);
    contentStream.showText(safeText("Payment is due within 30 days of issue.", fonts.standard()));
    contentStream.endText();

    contentStream.setNonStrokingColor(BRAND_R, BRAND_G, BRAND_B);
    contentStream.addRect(0, 0, 612, 22);
    contentStream.fill();
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

  private static String cleanControlChars(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    text.codePoints()
        .forEach(
            codePoint -> {
              if (codePoint == '\r' || codePoint == '\n' || Character.isISOControl(codePoint)) {
                sb.append(' ');
              } else {
                sb.appendCodePoint(codePoint);
              }
            });
    return sb.toString();
  }

  static String processBidi(String text) {
    if (text == null || text.isBlank()) {
      return text == null ? "" : text;
    }
    try {
      ArabicShaping shaper = new ArabicShaping(ArabicShaping.LETTERS_SHAPE);
      String shaped = shaper.shape(text);
      Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
      bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
      return bidi.writeReordered(Bidi.DO_MIRRORING);
    } catch (Exception e) {
      return text;
    }
  }

  static String safeText(String text, PDFont font) {
    if (text == null) {
      return "";
    }
    String bidiText = processBidi(cleanControlChars(text));
    StringBuilder sb = new StringBuilder();
    bidiText
        .codePoints()
        .forEach(
            codePoint -> {
              String charStr = new String(Character.toChars(codePoint));
              try {
                font.encode(charStr);
                sb.append(charStr);
              } catch (Exception e) {
                sb.append('?');
              }
            });
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
    String cleaned = cleanControlChars(text);
    if (cleaned.isBlank()) {
      return lines;
    }

    StringBuilder currentLine = new StringBuilder();
    for (String word : cleaned.split("\\s+")) {
      if (word.isEmpty()) {
        continue;
      }
      appendWordToLine(word, currentLine, lines, font, fontSize, maxWidth);
    }

    if (!currentLine.isEmpty()) {
      lines.add(safeText(currentLine.toString(), font));
    }

    return lines;
  }

  private static void appendWordToLine(
      String word,
      StringBuilder currentLine,
      List<String> lines,
      PDFont font,
      float fontSize,
      float maxWidth) {
    String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
    if (getTextWidth(safeText(testLine, font), font, fontSize) <= maxWidth) {
      currentLine.setLength(0);
      currentLine.append(testLine);
    } else {
      if (!currentLine.isEmpty()) {
        lines.add(safeText(currentLine.toString(), font));
        currentLine.setLength(0);
      }
      if (getTextWidth(safeText(word, font), font, fontSize) > maxWidth) {
        splitLongWord(word, currentLine, lines, font, fontSize, maxWidth);
      } else {
        currentLine.append(word);
      }
    }
  }

  private static void splitLongWord(
      String word,
      StringBuilder currentLine,
      List<String> lines,
      PDFont font,
      float fontSize,
      float maxWidth) {
    for (char c : word.toCharArray()) {
      if (getTextWidth(safeText(currentLine.toString() + c, font), font, fontSize) <= maxWidth) {
        currentLine.append(c);
      } else {
        if (!currentLine.isEmpty()) {
          lines.add(safeText(currentLine.toString(), font));
          currentLine.setLength(0);
        }
        currentLine.append(c);
      }
    }
  }
}
