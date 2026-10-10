package com.mandiwatch.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the CSV you get from data.gov.in ("Download CSV" on the mandi price dataset)
 * or any hand-made file with the same columns.
 */
@Component
public class CsvPriceParser {

    public List<RawPriceRow> parse(InputStream in) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .get();

        List<RawPriceRow> rows = new ArrayList<>();
        try (Reader reader = new InputStreamReader(skipBom(in), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            if (!looksLikePriceFile(headers)) {
                throw new IOException("This doesn't look like a mandi price file. Expected columns like "
                        + "State, District, Market, Commodity, Arrival_Date, Min_Price, Max_Price, Modal_Price. Found: "
                        + headers);
            }
            for (CSVRecord record : parser) {
                rows.add(RawPriceRow.fromMap(record.toMap()));
            }
        }
        return rows;
    }

    private static boolean looksLikePriceFile(List<String> headers) {
        List<String> keys = headers.stream().map(RawPriceRow::normaliseKey).toList();
        return keys.contains("market") && keys.contains("commodity") && keys.contains("modal_price");
    }

    /** Excel adds a byte-order mark to UTF-8 CSVs, which would corrupt the first header name. */
    private static InputStream skipBom(InputStream in) throws IOException {
        java.io.PushbackInputStream pb = new java.io.PushbackInputStream(in, 3);
        byte[] first = new byte[3];
        int n = pb.read(first, 0, 3);
        boolean bom = n == 3 && (first[0] & 0xFF) == 0xEF && (first[1] & 0xFF) == 0xBB && (first[2] & 0xFF) == 0xBF;
        if (!bom && n > 0) {
            pb.unread(first, 0, n);
        }
        return pb;
    }
}
