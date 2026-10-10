package com.mandiwatch.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvPriceParserTest {

    private final CsvPriceParser parser = new CsvPriceParser();

    @Test
    void parsesFileWithExcelBom() throws IOException {
        String csv = "﻿State,District,Market,Commodity,Variety,Arrival_Date,Min_Price,Max_Price,Modal_Price\n"
                + "Telangana,Hyderabad,Bowenpally,Tomato,Local,03/10/2026,1500,2100,1800\n"
                + "Telangana,Warangal,Warangal,\"Paddy(Dhan)(Common)\",Other,03/10/2026,2200,2350,2300\n";

        List<RawPriceRow> rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).state()).isEqualTo("Telangana");
        assertThat(rows.get(1).commodity()).isEqualTo("Paddy(Dhan)(Common)");
    }

    @Test
    void rejectsUnrelatedFile() {
        String csv = "name,age\nRavi,30\n";
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("doesn't look like a mandi price file");
    }
}
