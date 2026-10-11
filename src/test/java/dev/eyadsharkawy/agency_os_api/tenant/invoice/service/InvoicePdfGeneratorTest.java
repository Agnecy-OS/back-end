package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InvoicePdfGeneratorTest {

  private Client client;
  private Invoice invoice;
  private Project project1;
  private Project project2;

  @BeforeEach
  void setUp() {
    client = new Client();
    client.setId(UUID.randomUUID());
    client.setName("Globex Corp");
    client.setEmail("billing@globex.com");

    invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setClient(client);
    invoice.setStatus(InvoiceStatus.SENT);
    invoice.setTotalAmount(new BigDecimal("1500.00"));
    invoice.setCreatedAt(Instant.now());

    project1 = new Project();
    project1.setId(UUID.randomUUID());
    project1.setName("Mobile App");
    project1.setBillingRate(new BigDecimal("150.00"));

    project2 = new Project();
    project2.setId(UUID.randomUUID());
    project2.setName("Web Portal");
    project2.setBillingRate(new BigDecimal("120.00"));
  }

  static Stream<Arguments> entryScenarios() {
    return Stream.of(
        Arguments.of("Single project entry", 1, 120),
        Arguments.of("Multiple project entries", 2, 60),
        Arguments.of("Empty entries fallback", 0, 0),
        Arguments.of("Small 1-minute duration entry", 1, 1));
  }

  @ParameterizedTest(name = "generate should produce valid PDF for {0}")
  @MethodSource("entryScenarios")
  void generate_VariousEntryScenarios_Success(String scenario, int count, int duration)
      throws IOException {
    List<TimeEntry> entries = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Task task = new Task();
      task.setId(UUID.randomUUID());
      task.setTitle("Task #" + i);
      task.setProject(i % 2 == 0 ? project1 : project2);

      TimeEntry entry = new TimeEntry();
      entry.setId(UUID.randomUUID());
      entry.setTask(task);
      entry.setDurationMinutes(duration);
      entries.add(entry);
    }

    byte[] pdf = InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", entries);

    assertThat(pdf).isNotNull().hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("generate should handle multi-page overflow with many entries")
  void generate_MultiPageOverflow_Success() throws IOException {
    List<TimeEntry> entries = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      Task task = new Task();
      task.setId(UUID.randomUUID());
      task.setTitle("Long Task #" + i);
      task.setProject(project1);

      TimeEntry entry = new TimeEntry();
      entry.setId(UUID.randomUUID());
      entry.setTask(task);
      entry.setDurationMinutes(45);
      entries.add(entry);
    }

    byte[] pdf = InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", entries);

    assertThat(pdf).isNotNull().hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("formatDuration should smartly format minutes into clear readable strings")
  void formatDuration_AllCases() {
    assertThat(InvoicePdfGenerator.formatDuration(0)).isEqualTo("0 mins");
    assertThat(InvoicePdfGenerator.formatDuration(-5)).isEqualTo("0 mins");
    assertThat(InvoicePdfGenerator.formatDuration(1)).isEqualTo("1 min");
    assertThat(InvoicePdfGenerator.formatDuration(2)).isEqualTo("2 mins");
    assertThat(InvoicePdfGenerator.formatDuration(15)).isEqualTo("15 mins");
    assertThat(InvoicePdfGenerator.formatDuration(59)).isEqualTo("59 mins");
    assertThat(InvoicePdfGenerator.formatDuration(60)).isEqualTo("1 hr");
    assertThat(InvoicePdfGenerator.formatDuration(61)).isEqualTo("1 hr 1 min");
    assertThat(InvoicePdfGenerator.formatDuration(75)).isEqualTo("1 hr 15 mins");
    assertThat(InvoicePdfGenerator.formatDuration(120)).isEqualTo("2 hrs");
    assertThat(InvoicePdfGenerator.formatDuration(121)).isEqualTo("2 hrs 1 min");
    assertThat(InvoicePdfGenerator.formatDuration(135)).isEqualTo("2 hrs 15 mins");
  }

  @Test
  @DisplayName("generate should produce valid PDF when text contains Arabic and special characters")
  void generate_WithArabicAndSpecialCharacters_Success() throws IOException {
    client.setName("شركة الأهرام للتقنية");
    project1.setName("منظومة الخدمات السحابية");

    Task task = new Task();
    task.setId(UUID.randomUUID());
    task.setTitle("تصميم واجهة المستخدم وتطوير الواجهة الخلفية");
    task.setProject(project1);

    TimeEntry entry = new TimeEntry();
    entry.setId(UUID.randomUUID());
    entry.setTask(task);
    entry.setDurationMinutes(90);

    byte[] pdf =
        InvoicePdfGenerator.generate(
            invoice, "وكالة الإبداع الرقمي", "contact@agency.com", List.of(entry));

    assertThat(pdf).isNotNull().hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("generate should wrap long task titles across multiple lines without overflow error")
  void generate_WithLongTaskTitle_WrapsAndSucceeds() throws IOException {
    Task task = new Task();
    task.setId(UUID.randomUUID());
    task.setTitle(
        "This is an extremely long task description that should break across multiple lines in the Activity column instead of overflowing into the Rate or Amount column. It includes details about deployment, unit tests, end to end tests, and documentation.");
    task.setProject(project1);

    TimeEntry entry = new TimeEntry();
    entry.setId(UUID.randomUUID());
    entry.setTask(task);
    entry.setDurationMinutes(150);

    byte[] pdf =
        InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", List.of(entry));

    assertThat(pdf).isNotNull().hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("wrapText should break long lines into chunks within max width")
  void wrapText_SplitsIntoMultipleLines() throws Exception {
    try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
      try (java.io.InputStream is = getClass().getResourceAsStream("/fonts/Amiri-Regular.ttf")) {
        org.apache.pdfbox.pdmodel.font.PDType0Font font =
            org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, is);
        String longText =
            "This is a long activity line that should be wrapped into multiple lines properly because it exceeds the max allowed column width";
        List<String> wrapped = InvoicePdfGenerator.wrapText(longText, font, 10, 150f);
        assertThat(wrapped).hasSizeGreaterThan(1);
      }
    }
  }

  @Test
  @DisplayName("generate should render fallback line item when billedEntries is empty")
  void generate_EmptyBilledEntries_RendersFallbackRow() throws IOException {
    byte[] pdf =
        InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", List.of());
    assertThat(pdf).isNotNull().hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("generate should shape Arabic text and reorder BiDi for accurate rendering")
  void generate_WithArabicShapingAndBidi_RendersAccurately() throws Exception {
    String englishInput = "Development of user dashboard (45 mins)";
    String processedEn = InvoicePdfGenerator.processBidi(englishInput);
    assertThat(processedEn).isEqualTo(englishInput);

    String arabicInput = "حلب الجاموسة - شراء الجاموسة (5 mins)";
    String processedAr = InvoicePdfGenerator.processBidi(arabicInput);
    assertThat(processedAr).isNotEqualTo(arabicInput);

    Task task1 = new Task();
    task1.setId(UUID.randomUUID());
    task1.setTitle("حلب الجاموسة - شراء الجاموسة");
    task1.setProject(project1);

    TimeEntry entry1 = new TimeEntry();
    entry1.setId(UUID.randomUUID());
    entry1.setTask(task1);
    entry1.setDurationMinutes(5);

    Task task2 = new Task();
    task2.setId(UUID.randomUUID());
    task2.setTitle("حلب الجاموسة - شراء الجاموسة");
    task2.setProject(project1);

    TimeEntry entry2 = new TimeEntry();
    entry2.setId(UUID.randomUUID());
    entry2.setTask(task2);
    entry2.setDurationMinutes(296); // 4 hrs 56 mins

    byte[] invoicePdf =
        InvoicePdfGenerator.generate(
            invoice, "مزرعة البركة", "contact@baraka.com", List.of(entry1, entry2));
    assertThat(invoicePdf).isNotNull().hasSizeGreaterThan(0);
  }
}
