package org.roda.rodain.creation.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.StageStyle;
import org.roda.rodain.core.AppProperties;
import org.roda.rodain.core.I18n;
import org.roda.rodain.creation.CreateSips;
import org.roda.rodain.sip.SipPreview;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 19/11/2015.
 */
public class CreationModalProcessing extends BorderPane {
  private CreateSips creator;
  private static CreationModalStage stage;

  // top
  private Label subtitleSuccess, subtitleError;
  private String subtitleFormat;
  private String etaFormatHour, etaFormatHours, etaFormatMinute, etaFormatMinutes, etaFormatLessMin, etaFormatLess30;
  // center
  private ProgressBar progress;
  private Label sipName, sipAction, eta, etaLabel, elapsedTime;
  private HBox etaBox;
  private Timer timer;

  private HBox finishedBox;

  /**
   * Creates a pane to show the progress of the SIP exportation.
   *
   * @param creator
   *          The SIP creator object
   * @param stage
   *          The stage of the pane
   */
  public CreationModalProcessing(CreateSips creator, CreationModalStage stage) {
    this.creator = creator;
    CreationModalProcessing.stage = stage;

    etaFormatHour = String.format("< %%d %s ", I18n.t("CreationModalProcessing.hour"));
    etaFormatHours = String.format("< %%d %s ", I18n.t("CreationModalProcessing.hours"));
    etaFormatMinute = String.format("%%d %s", I18n.t("CreationModalProcessing.minute"));
    etaFormatMinutes = String.format("%%d %s", I18n.t("CreationModalProcessing.minutes"));
    etaFormatLessMin = I18n.t("CreationModalProcessing.lessMinute");
    etaFormatLess30 = I18n.t("CreationModalProcessing.lessSeconds");

    subtitleFormat = I18n.t("CreationModalProcessing.subtitle");

    getStyleClass().add("sipcreator");

    createTop();
    createCenter();
    createBottom();

    createUpdateTask();
  }

  private void createTop() {
    VBox top = new VBox(5);
    top.setPadding(new Insets(10, 10, 10, 0));
    top.getStyleClass().add("hbox");
    top.setAlignment(Pos.CENTER);

    Label title = new Label(I18n.t("CreationModalPreparation.creatingSips"));
    title.setId("title");

    top.getChildren().add(title);
    setTop(top);
  }

  private void createCenter() {
    VBox center = new VBox();
    center.setAlignment(Pos.CENTER_LEFT);
    center.setPadding(new Insets(0, 10, 10, 10));

    etaBox = new HBox(10);
    etaLabel = new Label(I18n.t("CreationModalProcessing.remaining"));
    etaLabel.setMinWidth(70);
    etaLabel.getStyleClass().add("boldText");
    eta = new Label();
    etaBox.getChildren().addAll(etaLabel, eta);

    HBox subtitles = new HBox(5);
    HBox space = new HBox();
    HBox.setHgrow(space, Priority.ALWAYS);

    subtitleSuccess = new Label("");
    subtitleSuccess.setId("subtitle");

    subtitleError = new Label("");
    subtitleError.setId("subtitle");

    subtitles.getChildren().addAll(subtitleSuccess, space, subtitleError);

    progress = new ProgressBar();
    progress.setPadding(new Insets(5, 0, 10, 0));
    progress.setPrefSize(380, 25);

    HBox elapsed = new HBox(10);
    elapsed.maxWidth(380);
    Label lElapsed = new Label(I18n.t("CreationModalProcessing.elapsed"));
    lElapsed.getStyleClass().add("boldText");
    lElapsed.setMinWidth(70);
    elapsedTime = new Label("");
    elapsed.getChildren().addAll(lElapsed, elapsedTime);

    HBox sip = new HBox(10);
    sip.maxWidth(380);
    Label lName = new Label(I18n.t("CreationModalProcessing.currentSip"));
    lName.getStyleClass().add("boldText");
    lName.setMinWidth(70);
    sipName = new Label("");
    sip.getChildren().addAll(lName, sipName);

    HBox action = new HBox(10);
    Label lAction = new Label(I18n.t("CreationModalProcessing.action"));
    lAction.getStyleClass().add("boldText");
    lAction.setMinWidth(70);
    sipAction = new Label("");
    action.getChildren().addAll(lAction, sipAction);

    center.getChildren().addAll(subtitles, progress, etaBox, elapsed, sip, action);
    setCenter(center);
  }

  private void createBottom() {
    HBox bottom = new HBox();
    bottom.setPadding(new Insets(0, 10, 10, 10));
    bottom.setAlignment(Pos.CENTER_LEFT);
    Button cancel = new Button(I18n.t("cancel"));
    cancel.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent actionEvent) {
        timer.cancel();
        creator.cancel();
        stage.close();
      }
    });

    bottom.getChildren().add(cancel);
    setBottom(bottom);

    finishedBox = new HBox();
    finishedBox.setPadding(new Insets(0, 10, 10, 10));
    finishedBox.setAlignment(Pos.CENTER_RIGHT);
    Button close = new Button(I18n.t("close"));

    close.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent actionEvent) {
        stage.close();
      }
    });

    finishedBox.getChildren().add(close);
  }

  private void createUpdateTask() {
    TimerTask updater = new TimerTask() {
      @Override
      public void run() {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            int created = creator.getCreatedSipsCount();
            int size = creator.getSipsCount();
            int errors = creator.getErrorCount();
            double etaDouble = creator.getTimeRemainingEstimate();
            long startedTime = creator.getStartedTime();
            updateETA(etaDouble);
            double prog = creator.getProgress();

            if (errors > 0) {
              subtitleError.setText(errors + I18n.t("CreationModalProcessing.errors"));
            }
            subtitleSuccess.setText(String.format(subtitleFormat, created, size, (int) (prog * 100)));
            progress.setProgress(prog);

            sipName.setText(creator.getSipName());
            sipAction.setText(creator.getAction());
            // format elapsed time
            long millis = System.currentTimeMillis() - startedTime;
            long second = (millis / 1000) % 60;
            long minute = (millis / (1000 * 60)) % 60;
            long hour = (millis / (1000 * 60 * 60)) % 24;
            elapsedTime.setText(String.format("%02d:%02d:%02d", hour, minute, second));

            // stop the timer when all the SIPs have been created
            if ((created + errors) == size) {
              eta.setText(I18n.t("CreationModalProcessing.finished"));
              progress.setProgress(100);
              finished();
            }
          }
        });
      }
    };

    timer = new Timer();
    timer.schedule(updater, 0, 200);
  }

  private void updateETA(double etaDouble) {
    if (etaDouble >= 0) {
      if (etaBox.getChildren().isEmpty()) {
        etaBox.getChildren().addAll(etaLabel, eta);
      }
      int second = (int) ((etaDouble / 1000) % 60);
      int minute = (int) ((etaDouble / (1000 * 60)) % 60);
      int hour = (int) ((etaDouble / (1000 * 60 * 60)) % 24);
      String result;
      if (hour > 0) {
        if (hour == 1)
          result = String.format(etaFormatHour, hour);
        else
          result = String.format(etaFormatHours, hour);
      } else if (minute > 0) {
        if (minute == 1)
          result = String.format(etaFormatMinute, minute);
        else
          result = String.format(etaFormatMinutes, minute);
      } else if (second > 30) {
        result = etaFormatLessMin;
      } else
        result = etaFormatLess30;
      eta.setText(result);
    } else {
      etaBox.getChildren().clear();
      eta.setText(I18n.t("CreationModalProcessing.impossibleEstimate"));
    }
  }

  private void finished() {
    timer.cancel();
    setBottom(finishedBox);
  }

  /**
   * Shows an alert with the error message regarding an exception found when
   * exporting a SIP.
   * 
   * @param sip
   *          The SIP being exported when the exception was thrown
   * @param ex
   *          The thrown exception
   */
  public static void showError(SipPreview sip, Exception ex) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.initStyle(StageStyle.UNDECORATED);
      alert.initOwner(stage);
      alert.setTitle(I18n.t("CreationModalProcessing.alert.title"));
      String header = String.format(I18n.t("CreationModalProcessing.alert.header"), sip.getTitle());
      alert.setHeaderText(header);
      StringBuilder content = new StringBuilder(ex.getLocalizedMessage());
      content.append("\n");
      content.append(I18n.t("CreationModalProcessing.cause"));
      content.append(": ").append(ex.getCause().getLocalizedMessage());
      alert.setContentText(content.toString());
      alert.getDialogPane().setStyle(AppProperties.getStyle("export.alert"));

      // Create expandable Exception.
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println(ex.getMessage());
      for (StackTraceElement ste : ex.getStackTrace()) {
        pw.println("\t" + ste);
      }
      String exceptionText = sw.toString();

      Label label = new Label(I18n.t("CreationModalProcessing.alert.stacktrace"));

      TextArea textArea = new TextArea(exceptionText);
      textArea.setEditable(false);
      textArea.minWidthProperty().bind(alert.getDialogPane().widthProperty().subtract(20));
      textArea.maxWidthProperty().bind(alert.getDialogPane().widthProperty().subtract(20));

      GridPane expContent = new GridPane();
      expContent.setMaxWidth(Double.MAX_VALUE);
      expContent.add(label, 0, 0);
      expContent.add(textArea, 0, 1);

      textArea.minHeightProperty().bind(expContent.heightProperty().subtract(20));
      // Set expandable Exception into the dialog pane.
      alert.getDialogPane().setExpandableContent(expContent);
      alert.getDialogPane().minWidthProperty().bind(stage.widthProperty());
      alert.getDialogPane().minHeightProperty().bind(stage.heightProperty());

      // Without this setStyle the pane won't resize correctly. Black magic...
      alert.getDialogPane().setStyle(AppProperties.getStyle("creationmodalprocessing.blackmagic"));

      alert.show();
    });
  }
}
