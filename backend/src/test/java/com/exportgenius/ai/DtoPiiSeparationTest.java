package com.exportgenius.ai;

import com.exportgenius.ai.dto.exporter.ExporterDealDTO;
import com.exportgenius.ai.dto.importer.ImporterDealDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class DtoPiiSeparationTest {

    @Test
    public void testExporterDealDtoHasNoImportersPiiOrSellPrice() {
        List<String> fieldNames = Arrays.stream(ExporterDealDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertFalse(fieldNames.contains("sellPrice"), 
                "Security Leak: ExporterDealDTO contains 'sellPrice' which must be hidden from Exporters");
        assertFalse(fieldNames.contains("importerName"), 
                "Security Leak: ExporterDealDTO contains 'importerName' which must be hidden from Exporters");
        assertFalse(fieldNames.contains("importerCountry"), 
                "Security Leak: ExporterDealDTO contains 'importerCountry' which must be hidden from Exporters");
        assertFalse(fieldNames.contains("marginAmount"), 
                "Security Leak: ExporterDealDTO contains 'marginAmount' which must be hidden from Exporters");
    }

    @Test
    public void testImporterDealDtoHasNoExportersPiiOrSupplyPrice() {
        List<String> fieldNames = Arrays.stream(ImporterDealDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertFalse(fieldNames.contains("supplyPrice"), 
                "Security Leak: ImporterDealDTO contains 'supplyPrice' which must be hidden from Importers");
        assertFalse(fieldNames.contains("exporterName"), 
                "Security Leak: ImporterDealDTO contains 'exporterName' which must be hidden from Importers");
        assertFalse(fieldNames.contains("exporterCountry"), 
                "Security Leak: ImporterDealDTO contains 'exporterCountry' which must be hidden from Importers");
        assertFalse(fieldNames.contains("marginAmount"), 
                "Security Leak: ImporterDealDTO contains 'marginAmount' which must be hidden from Importers");
    }
}
