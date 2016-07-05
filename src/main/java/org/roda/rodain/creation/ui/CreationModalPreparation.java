package org.roda.rodain.creation.ui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import org.controlsfx.control.ToggleSwitch;
import org.roda.rodain.core.AppProperties;
import org.roda.rodain.core.I18n;
import org.roda.rodain.core.RodaIn;
import org.roda.rodain.creation.SipTypes;
import org.roda.rodain.utils.UIPair;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 19/11/2015.
 */
public class CreationModalPreparation extends BorderPane {
  private final int DEFAULT_WIDTH = 120;
  public enum NAME_TYPES {ID, TITLE_ID, TITLE_DATE}
  private CreationModalStage stage;

  private static Path outputFolder;
  private ComboBox<String> sipTypes;
  private ComboBox<UIPair> nameTypes;
  private static Button start;
  private int selected, allCount;
  private ToggleSwitch toggleSwitch;
  private TextField prefixField;

  /**
   * Creates a modal to prepare for the SIP exportation.
   * <p/>
   * <p>
   * This class creates a pane with a field to choose what the output directory
   * for the SIP exportation should be and the format of the SIPs.
   * </p>
   *
   * @param stage The stage of the pane
   */
  public CreationModalPreparation(CreationModalStage stage) {
    this.stage = stage;

    getStyleClass().add("sipcreator");

    createTop();
    createCenter();
    createBottom();
  }

  private void createTop() {
    VBox top = new VBox(5);
    top.getStyleClass().add("hbox");
    top.setPadding(new Insets(10, 10, 10, 0));
    top.setAlignment(Pos.CENTER);

    Label title = new Label(I18n.t("CreationModalPreparation.creatingSips"));
    title.setId("title");

    top.getChildren().add(title);
    setTop(top);
  }

  private void createCenter() {
    VBox center = new VBox(5);
    center.setAlignment(Pos.CENTER_LEFT);
    center.setPadding(new Insets(10, 10, 10, 10));

    VBox countBox = createCountBox();
    HBox outputFolderBox = createOutputFolder();
    HBox sipTypesBox = createSipTypes();
    HBox prefixBox = createNameBox();

    center.getChildren().addAll(countBox, outputFolderBox, sipTypesBox, prefixBox);
    setCenter(center);
  }

  private VBox createCountBox() {
    VBox countBox = new VBox(10);
    countBox.setAlignment(Pos.CENTER);
    selected = RodaIn.getSelectedSipPreviews().size();
    allCount = RodaIn.getAllSipPreviews().size();

    Label countLabel = new Label(String.format("%s %d/%d", I18n.t("selected"), selected, allCount));
    countLabel.getStyleClass().add("prepareCreationSubtitle");
    toggleSwitch = new ToggleSwitch(I18n.t("CreationModalPreparation.exportAll"));

    if (selected == 0 || selected == allCount)
      toggleSwitch.setSelected(true);

    countBox.getChildren().addAll(countLabel, toggleSwitch);
    return countBox;
  }

  private HBox createOutputFolder() {
    HBox outputFolderBox = new HBox(5);
    outputFolderBox.setAlignment(Pos.CENTER_LEFT);
    HBox space = new HBox();
    HBox.setHgrow(space, Priority.ALWAYS);

    Label outputFolderLabel = new Label(I18n.t("CreationModalPreparation.outputDirectory"));
    Button chooseFile = new Button(I18n.t("CreationModalPreparation.choose"));
    chooseFile.setMinWidth(DEFAULT_WIDTH);
    chooseFile.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.t("directorychooser.title"));
        File selectedFile = chooser.showDialog(stage);
        if (selectedFile == null)
          return;
        outputFolder = selectedFile.toPath();
        chooseFile.setText(selectedFile.toPath().getFileName().toString());
        start.setDisable(false);
      }
    });

    outputFolderBox.getChildren().addAll(outputFolderLabel, space, chooseFile);
    return outputFolderBox;
  }

  private HBox createNameBox() {
    HBox prefixBox = new HBox(5);
    prefixBox.setAlignment(Pos.CENTER_LEFT);
    HBox space = new HBox();
    HBox.setHgrow(space, Priority.ALWAYS);

    Label prefixLabel = new Label(I18n.t("CreationModalPreparation.prefix"));
    prefixField = new TextField();
    prefixField.setMinWidth(DEFAULT_WIDTH);
    prefixField.setMaxWidth(DEFAULT_WIDTH);
    prefixField.setText(AppProperties.getConfig("export.last_prefix"));

    nameTypes = new ComboBox<>();
    nameTypes.setMinWidth(DEFAULT_WIDTH);
    for (NAME_TYPES name_type : NAME_TYPES.values()) {
      nameTypes.getItems().add(new UIPair(name_type, I18n.t("name_types." + name_type)));
    }
    nameTypes.getSelectionModel().selectFirst();

    prefixBox.getChildren().addAll(prefixLabel, space, prefixField, nameTypes);
    return prefixBox;
  }

  /**
   * Sets the output folder of the SIPs. Used for testing.
   *
   * @param out The path of the output folder
   */
  public static void setOutputFolder(String out) {
    if (out != null && !"".equals(out)) {
      outputFolder = Paths.get(out);
      start.setDisable(false);
    }
  }

  private HBox createSipTypes() {
    HBox sipTypesBox = new HBox(5);
    sipTypesBox.setAlignment(Pos.CENTER_LEFT);
    HBox space = new HBox();
    HBox.setHgrow(space, Priority.ALWAYS);

    Label sipTypesLabel = new Label(I18n.t("CreationModalPreparation.sipFormat"));

    sipTypes = new ComboBox<>();
    sipTypes.setMinWidth(DEFAULT_WIDTH);
    sipTypes.setId("sipTypes");
    sipTypes.getItems().addAll("BagIt", "E-ARK");
    sipTypes.getSelectionModel().select("E-ARK");

    sipTypesBox.getChildren().addAll(sipTypesLabel, space, sipTypes);
    return sipTypesBox;
  }

  private void createBottom() {
    HBox bottom = new HBox();
    bottom.setPadding(new Insets(0, 10, 10, 10));
    bottom.setAlignment(Pos.CENTER_LEFT);

    HBox space = new HBox();
    HBox.setHgrow(space, Priority.ALWAYS);

    Button cancel = new Button(I18n.t("cancel"));
    cancel.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent actionEvent) {
        stage.close();
      }
    });

    start = new Button(I18n.t("start"));
    start.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent actionEvent) {
        if (outputFolder == null)
          return;
        String selectedType = sipTypes.getSelectionModel().getSelectedItem();
        SipTypes type;
        if ("BagIt".equals(selectedType))
          type = SipTypes.BAGIT;
        else
          type = SipTypes.EARK;

        AppProperties.setConfig("export.last_prefix", prefixField.getText());
        AppProperties.saveConfig();
        stage.startCreation(outputFolder, type, toggleSwitch.isSelected(), prefixField.getText(), (NAME_TYPES) nameTypes.getSelectionModel().getSelectedItem().getKey());
      }
    });

    start.setDisable(true);

    bottom.getChildren().addAll(cancel, space, start);
    setBottom(bottom);
  }
}
