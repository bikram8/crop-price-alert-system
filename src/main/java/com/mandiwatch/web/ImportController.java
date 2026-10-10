package com.mandiwatch.web;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.ImportRun;
import com.mandiwatch.repository.ImportRunRepository;
import com.mandiwatch.service.ApiImportService;
import com.mandiwatch.service.CsvPriceParser;
import com.mandiwatch.service.PriceImportService;
import com.mandiwatch.service.RawPriceRow;
import com.mandiwatch.service.SampleDataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class ImportController {

    private final PriceImportService importService;
    private final ApiImportService apiImportService;
    private final CsvPriceParser csvParser;
    private final SampleDataService sampleDataService;
    private final ImportRunRepository importRunRepository;
    private final MandiProperties properties;

    public ImportController(PriceImportService importService,
                            ApiImportService apiImportService,
                            CsvPriceParser csvParser,
                            SampleDataService sampleDataService,
                            ImportRunRepository importRunRepository,
                            MandiProperties properties) {
        this.importService = importService;
        this.apiImportService = apiImportService;
        this.csvParser = csvParser;
        this.sampleDataService = sampleDataService;
        this.importRunRepository = importRunRepository;
        this.properties = properties;
    }

    @GetMapping("/import")
    public String page(Model model) {
        model.addAttribute("runs", importRunRepository.findTop20ByOrderByStartedAtDesc());
        model.addAttribute("apiKeySet", properties.api().hasKey());
        model.addAttribute("apiState", properties.api().state());
        model.addAttribute("scheduled", properties.api().scheduled());
        model.addAttribute("cron", properties.api().cron());
        return "import";
    }

    @PostMapping("/import/csv")
    public String uploadCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose a CSV file first.");
            return "redirect:/import";
        }
        try {
            List<RawPriceRow> rows = csvParser.parse(file.getInputStream());
            String source = "CSV upload: " + file.getOriginalFilename();
            ImportRun run = importService.importRows(source, rows);
            redirect.addFlashAttribute("message", summary(run));
        } catch (IOException e) {
            importService.recordFailure("CSV upload: " + file.getOriginalFilename(), e.getMessage());
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/import";
    }

    @PostMapping("/import/api")
    public String runApiImport(RedirectAttributes redirect) {
        ImportRun run = apiImportService.runImport();
        if (run.isSuccess()) {
            redirect.addFlashAttribute("message", summary(run));
        } else {
            redirect.addFlashAttribute("error", "API import failed: " + run.getNote());
        }
        return "redirect:/import";
    }

    @GetMapping("/import/demo-bulletin.csv")
    public ResponseEntity<byte[]> demoBulletin() {
        byte[] body = sampleDataService.demoBulletinCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"demo-bulletin.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    private String summary(ImportRun run) {
        // alertsSent is filled in by the alert listener after commit, so ask the DB
        Integer alerts = importRunRepository.findAlertsSent(run.getId());
        String text = "Imported " + run.getRowsSaved() + " of " + run.getRowsRead() + " rows ("
                + run.getRowsSkipped() + " skipped). SMS alerts sent: " + (alerts == null ? 0 : alerts) + ".";
        return run.getNote() == null ? text : text + " " + run.getNote();
    }
}
