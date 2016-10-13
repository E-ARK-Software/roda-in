package org.roda.rodain.creation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.roda.rodain.core.AppProperties;
import org.roda.rodain.core.I18n;
import org.roda.rodain.creation.ui.CreationModalPreparation;
import org.roda.rodain.creation.ui.CreationModalProcessing;
import org.roda.rodain.rules.MetadataOptions;
import org.roda.rodain.rules.TreeNode;
import org.roda.rodain.schema.DescObjMetadata;
import org.roda.rodain.schema.DescriptionObject;
import org.roda.rodain.sip.SipPreview;
import org.roda.rodain.sip.SipRepresentation;
import org.roda_project.commons_ip.model.IPContentType;
import org.roda_project.commons_ip.model.IPDescriptiveMetadata;
import org.roda_project.commons_ip.model.IPFile;
import org.roda_project.commons_ip.model.IPRepresentation;
import org.roda_project.commons_ip.model.MetadataType;
import org.roda_project.commons_ip.model.RepresentationContentType;
import org.roda_project.commons_ip.model.SIP;
import org.roda_project.commons_ip.model.SIPObserver;
import org.roda_project.commons_ip.model.impl.eark.EARKSIP;
import org.roda_project.commons_ip.utils.IPEnums;
import org.roda_project.commons_ip.utils.SIPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 19/11/2015.
 */
public class EarkSipCreator extends SimpleSipCreator implements SIPObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(EarkSipCreator.class.getName());
  private int countFilesOfZip;
  private int currentSIPadded = 0;
  private int currentSIPsize = 0;
  private int repProcessingSize;

  private String prefix;
  private CreationModalPreparation.NAME_TYPES name_type;

  /**
   * Creates a new EARK SIP exporter.
   *
   * @param outputPath
   *          The path to the output folder of the SIP exportation
   * @param previews
   *          The map with the SIPs that will be exported
   */
  public EarkSipCreator(Path outputPath, Map<DescriptionObject, List<String>> previews, String prefix, CreationModalPreparation.NAME_TYPES name_type) {
    super(outputPath, previews);
    this.prefix = prefix;
    this.name_type = name_type;
  }

  /**
   * Attempts to create an EARK SIP of each SipPreview
   */
  @Override
  public void run() {
    for (DescriptionObject preview : previews.keySet()) {
      if (canceled) {
        break;
      }
      createEarkSip(preview);
    }
    currentAction = I18n.t("done");
  }

  private void createEarkSip(DescriptionObject descriptionObject) {
    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
    try {
      IPContentType contentType = descriptionObject instanceof SipPreview ? ((SipPreview) descriptionObject).getContentType() : IPContentType.getMIXED();
      SIP earkSip = new EARKSIP(descriptionObject.getId(), contentType, agent_name);
      earkSip.addObserver(this);
      earkSip.setStatus(IPEnums.IPStatus.NEW);
      earkSip.setAncestors(previews.get(descriptionObject));

      currentSipProgress = 0;
      currentSipName = descriptionObject.getTitle();
      currentAction = actionCopyingMetadata;

      for (DescObjMetadata descObjMetadata : descriptionObject.getMetadata()) {
        String keyMetadataTypeValue = "metadata.type." + descObjMetadata.getMetadataType() + ".value";
        String metadataTypeString = AppProperties.getConfig(keyMetadataTypeValue);
        MetadataType metadataType = new MetadataType(MetadataType.MetadataTypeEnum.OTHER);

        Path schemaPath = AppProperties.getSchemaPath(descObjMetadata.getMetadataType());
        if (schemaPath != null)
          earkSip.addSchema(new IPFile(schemaPath));
        // Check if one of the values from the enum can be used
        if (metadataTypeString != null) {
          for (MetadataType.MetadataTypeEnum val : MetadataType.MetadataTypeEnum.values()) {
            if (metadataTypeString.equalsIgnoreCase(val.getType())) {
              metadataType = new MetadataType(val);
              break;
            }
          }
        }
        // If no value was found previously, set the Other type
        if (metadataType.getType() == MetadataType.MetadataTypeEnum.OTHER) {
          metadataType.setOtherType(metadataTypeString);
        }

        Path metadataPath = null;
        if (descObjMetadata.getCreatorOption() != MetadataOptions.TEMPLATE
          && descObjMetadata.getCreatorOption() != MetadataOptions.NEW_FILE && !descObjMetadata.isLoaded()) {
          metadataPath = descObjMetadata.getPath();
        }

        if (metadataPath == null) {
          String content = descriptionObject.getMetadataWithReplaces(descObjMetadata);

          metadataPath = tempDir.resolve(descObjMetadata.getMetadataType()+".xml");
          FileUtils.writeStringToFile(metadataPath.toFile(), content, "UTF-8");
        }

        IPFile metadataFile = new IPFile(metadataPath);
        IPDescriptiveMetadata metadata = new IPDescriptiveMetadata(metadataFile, metadataType,
          descObjMetadata.getVersion());
        earkSip.addDescriptiveMetadata(metadata);
      }

      currentAction = actionCopyingData;
      if(descriptionObject instanceof SipPreview) {
        SipPreview sip = (SipPreview) descriptionObject;
        for (SipRepresentation sr : sip.getRepresentations()) {
          IPRepresentation rep = new IPRepresentation(sr.getName());
          rep
              .setContentType(new RepresentationContentType(RepresentationContentType.RepresentationContentTypeEnum.MIXED));
          Set<TreeNode> files = sr.getFiles();
          currentSIPadded = 0;
          currentSIPsize = 0;
          // count files
          for (TreeNode tn : files) {
            currentSIPsize += tn.getFullTreePaths().size();
          }
          // add files to representation
          for (TreeNode tn : files) {
            addFileToRepresentation(tn, new ArrayList<>(), rep);
          }

          earkSip.addRepresentation(rep);
        }

        currentAction = I18n.t("SimpleSipCreator.documentation");
        Set<TreeNode> docs = sip.getDocumentation();
        for (TreeNode tn : docs) {
          addDocToZip(tn, new ArrayList<>(), earkSip);
        }
      }

      currentAction = I18n.t("SimpleSipCreator.initZIP");

      earkSip.build(outputPath, createSipName(descriptionObject));

      createdSipsCount++;
    } catch (SIPException e) {
      LOGGER.error("Commons IP exception", e);
      unsuccessful.add(descriptionObject);
      CreationModalProcessing.showError(descriptionObject, e);
    } catch (InterruptedException e) {
      canceled = true;
    } catch (IOException e) {
      LOGGER.error("Error accessing the files", e);
      unsuccessful.add(descriptionObject);
      CreationModalProcessing.showError(descriptionObject, e);
    } catch (Exception e) {
      LOGGER.error("Error exporting E-ARK SIP", e);
      unsuccessful.add(descriptionObject);
      CreationModalProcessing.showError(descriptionObject, e);
    }
  }

  private String createSipName(DescriptionObject sip){
    StringBuilder name = new StringBuilder();
    if(prefix != null && !"".equals(prefix)){
      name.append(prefix).append(" - ");
    }
    switch (name_type){
      case TITLE_ID:
        name.append(sip.getTitle());
        name.append(" - ");
        name.append(sip.getId());
        break;
      case TITLE_DATE:
        name.append(sip.getTitle());
        name.append(" - ");
        name.append(new SimpleDateFormat("yyyy.MM.dd HH.mm.ss.SSS").format(new Date()));
        break;
      case ID:
      default:
        name.append(sip.getId());
        break;
    }

    return name.toString();
  }

  private void addFileToRepresentation(TreeNode tn, List<String> relativePath, IPRepresentation rep) {
    if (Files.isDirectory(tn.getPath())) {
      // add this directory to the path list
      List<String> newRelativePath = new ArrayList<>(relativePath);
      newRelativePath.add(tn.getPath().getFileName().toString());
      // recursive call to all the node's children
      for (TreeNode node : tn.getChildren().values()) {
        addFileToRepresentation(node, newRelativePath, rep);
      }
    } else {
      // if it's a file, add it to the representation
      rep.addFile(tn.getPath(), relativePath);
      currentSIPadded++;
      String format = String.format("%s %s", actionCopyingData, "(%d/%d)");
      currentAction = String.format(format, currentSIPadded, currentSIPsize);
    }
  }

  private void addDocToZip(TreeNode tn, List<String> relativePath, SIP earkSip) {
    if (Files.isDirectory(tn.getPath())) {
      // add this directory to the path list
      List<String> newRelativePath = new ArrayList<>(relativePath);
      newRelativePath.add(tn.getPath().getFileName().toString());
      // recursive call to all the node's children
      for (TreeNode node : tn.getChildren().values()) {
        addDocToZip(node, newRelativePath, earkSip);
      }
    } else {
      // if it's a file, add it to the SIP
      IPFile fileDoc = new IPFile(tn.getPath(), relativePath);
      earkSip.addDocumentation(fileDoc);
    }
  }

  @Override
  public void sipBuildRepresentationsProcessingStarted(int i) {

  }

  @Override
  public void sipBuildRepresentationProcessingStarted(int size) {
    repProcessingSize = size;
  }

  @Override
  public void sipBuildRepresentationProcessingCurrentStatus(int i) {
    String format = I18n.t("CreationModalProcessing.representation") + " (%d/%d)";
    currentAction = String.format(format, i, repProcessingSize);
  }

  @Override
  public void sipBuildRepresentationProcessingEnded() {

  }

  @Override
  public void sipBuildRepresentationsProcessingEnded() {

  }

  @Override
  public void sipBuildPackagingStarted(int current) {
    countFilesOfZip = current;
  }

  @Override
  public void sipBuildPackagingCurrentStatus(int current) {
    String format = I18n.t("CreationModalProcessing.eark.progress");
    String progress = String.format(format, current, countFilesOfZip);
    currentAction = progress;
    currentSipProgress = ((float) current) / countFilesOfZip;
    currentSipProgress /= sipPreviewCount;
  }

  @Override
  public void sipBuildPackagingEnded() {
    currentAction = actionFinalizingSip;
    currentSipProgress = 0;
  }
}
