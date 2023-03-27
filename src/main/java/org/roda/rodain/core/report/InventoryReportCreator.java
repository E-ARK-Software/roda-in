package org.roda.rodain.core.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.roda.rodain.core.Constants;
import org.roda_project.commons_ip.model.SIP;
import org.roda_project.commons_ip.utils.METSZipEntryInfo;
import org.roda_project.commons_ip.utils.ZipEntryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.reader.BagReader;

public class InventoryReportCreator {
  private static final Logger LOGGER = LoggerFactory.getLogger(InventoryReportCreator.class.getName());

  private static final String CSV_FIELD_FILE_SIZE = "size";
  private static final String CSV_FIELD_FILE_CHECKSUM = "checksum";
  private static final String CSV_FIELD_FILE_CHECKSUM_TYPE = "checksum_type";
  private static final String CSV_FIELD_FILE_ABSOLUTE_PATH = "absolute path";
  private static final String CSV_FIELD_FILE_RELATIVE_ZIP_PATH = "zip relative path";
  private static final String CSV_FIELD_SIP_ID = "SIP ID";

  private Path outputPath;

  public InventoryReportCreator(Path outputPath) {
    this.outputPath = outputPath;
  }

  public void start(Map<Path, Object> sips) {
    CSVPrinter csvFilePrinter = null;
    CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator());
    BufferedWriter fileWriter = null;

    try {
      StringBuffer name = new StringBuffer();
      name.append("inventory_report");
      name.append(" - ");
      name.append(new SimpleDateFormat(Constants.DATE_FORMAT_1).format(new Date()));
      name.append(".csv");
      Path csvTempFile = outputPath.resolve(name.toString());

      fileWriter = Files.newBufferedWriter(csvTempFile);
      csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
      List<String> headers = new ArrayList<String>();
      headers.add(CSV_FIELD_SIP_ID);
      headers.add(CSV_FIELD_FILE_RELATIVE_ZIP_PATH);
      headers.add(CSV_FIELD_FILE_ABSOLUTE_PATH);
      headers.add(CSV_FIELD_FILE_CHECKSUM_TYPE);
      headers.add(CSV_FIELD_FILE_CHECKSUM);
      headers.add(CSV_FIELD_FILE_SIZE);
      csvFilePrinter.printRecord(headers);
      for (Map.Entry<Path, Object> entry : sips.entrySet()) {
        Object sipToProcess = entry.getValue();
        List<List<String>> lines = null;
        if (sipToProcess instanceof Bag) {
          lines = bagToCSVLines(entry.getKey());
        } else if (sipToProcess instanceof SIP) {
          SIP sip = (SIP) sipToProcess;
          lines = generateCsvLines(entry.getKey(), sip.getZipEntries().values());
        } else if (sipToProcess instanceof org.roda_project.commons_ip2.model.SIP) {
          org.roda_project.commons_ip2.model.SIP sip = (org.roda_project.commons_ip2.model.SIP) sipToProcess;
          lines = generateCsvLines(entry.getKey(), sip.getZipEntries().values());
        }

        if (lines != null) {
          csvFilePrinter.printRecords(lines);
        }
      }
    } catch (IOException | MaliciousPathException | UnparsableVersionException | UnsupportedAlgorithmException
      | InvalidBagitFileFormatException e) {
      LOGGER.error("Error creating inventory report", e);
    } finally {
      IOUtils.closeQuietly(fileWriter);
      IOUtils.closeQuietly(csvFilePrinter);
    }
  }

  private List<List<String>> generateCsvLines(Path path, Collection<ZipEntryInfo> values) {
    List<List<String>> lines = new ArrayList<List<String>>();
    for (ZipEntryInfo entry : values) {
      if (!(entry instanceof METSZipEntryInfo)) {
        try {
          List<String> line = new ArrayList<String>();
          // FIXME 20170310 hsilva: the following will fail for temp. files that
          // were most certainly deleted before getting to this point of sip
          // generation
          Long size = Files.size(entry.getFilePath());
          line.add(path.getFileName().toString());
          line.add(entry.getName());
          line.add(entry.getFilePath().toString());
          line.add(entry.getChecksumAlgorithm());
          line.add(entry.getChecksum());
          line.add(Long.toString(size));
          lines.add(line);
        } catch (IOException e) {
          LOGGER.debug("Error calculating file size", e);
        }
      }
    }
    return lines;
  }

  private List<List<String>> bagToCSVLines(Path path) throws MaliciousPathException, UnparsableVersionException,
    UnsupportedAlgorithmException, InvalidBagitFileFormatException, IOException {

    BagReader bagReader = new BagReader();
    Bag bag = bagReader.read(path);

    List<List<String>> lines = new ArrayList<List<String>>();
    for (Manifest manifest : bag.getTagManifests()) {
      for (Map.Entry<Path, String> entry : manifest.getFileToChecksumMap().entrySet()) {
        List<String> line = new ArrayList<>();
        long size = Files.size(entry.getKey());
        line.add(path.getFileName().toString());
        line.add(entry.getKey().toString());
        line.add("");
        line.add(manifest.getAlgorithm().getBagitName());
        line.add(entry.getValue());
        line.add(Long.toString(size));
        lines.add(line);
      }
    }
    for (Manifest manifest : bag.getPayLoadManifests()) {
      for (Map.Entry<Path, String> entry : manifest.getFileToChecksumMap().entrySet()) {
        List<String> line = new ArrayList<>();
        long size = Files.size(entry.getKey());
        line.add(path.getFileName().toString());
        line.add(entry.getKey().toString());
        line.add("");
        line.add(manifest.getAlgorithm().getBagitName());
        line.add(entry.getValue());
        line.add(Long.toString(size));
        lines.add(line);
      }
    }
    return lines;
  }
}
