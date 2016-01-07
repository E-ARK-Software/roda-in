package org.roda.rodain.creation.ui;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.roda.rodain.core.AppProperties;
import org.roda.rodain.creation.CreateSips;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 19/11/2015.
 */
public class CreationModalProcessing extends BorderPane {
  private CreateSips creator;
  private CreationModalStage stage;

  // top
  private Label subtitleSuccess, subtitleError;
  private String subtitleFormat = "Created %d of %d (%d%%)";
  private String etaFormatHour, etaFormatMinute, etaFormatSecond;
  // center
  private ProgressBar progress;
  private Label sipName, sipAction, eta;
  private Timer timer;

  private HBox finishedBox;

  /**
   * Creates a pane to show the progress of the SIP exportation.
   *
   * @param creator The SIP creator object
   * @param stage   The stage of the pane
   */
  public CreationModalProcessing(CreateSips creator, CreationModalStage stage) {
    this.creator = creator;
    this.stage = stage;

    etaFormatHour = String.format("%%s %s %%s %s %%s %s",
      AppProperties.getLocalizedString("CreationModalProcessing.hours"),
      AppProperties.getLocalizedString("CreationModalProcessing.minutes"),
      AppProperties.getLocalizedString("CreationModalProcessing.seconds"));

    etaFormatMinute = String.format("%%s %s %%s %s",
      AppProperties.getLocalizedString("CreationModalProcessing.minutes"),
      AppProperties.getLocalizedString("CreationModalProcessing.seconds"));

    etaFormatSecond = String.format("%%s %s", AppProperties.getLocalizedString("CreationModalProcessing.seconds"));

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

    Label title = new Label(AppProperties.getLocalizedString("CreationModalPreparation.creatingSips"));
    title.setId("title");

    top.getChildren().add(title);
    setTop(top);
  }

  private void createCenter() {
    VBox center = new VBox();
    center.setAlignment(Pos.CENTER_LEFT);
    center.setPadding(new Insets(0, 10, 10, 10));

    HBox etaBox = new HBox(10);
    Label etaLabel = new Label("Remaining time");
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

    HBox sip = new HBox(20);
    sip.maxWidth(380);
    Label lName = new Label(AppProperties.getLocalizedString("CreationModalProcessing.currentSip"));
    lName.setMinWidth(80);
    sipName = new Label("");
    sip.getChildren().addAll(lName, sipName);

    HBox action = new HBox(20);
    Label lAction = new Label(AppProperties.getLocalizedString("CreationModalProcessing.action"));
    lAction.setMinWidth(80);
    sipAction = new Label("");
    action.getChildren().addAll(lAction, sipAction);

    center.getChildren().addAll(subtitles, progress, etaBox, sip, action);
    setCenter(center);
  }

  private void createBottom() {
    HBox bottom = new HBox();
    bottom.setPadding(new Insets(0, 10, 10, 10));
    bottom.setAlignment(Pos.CENTER_LEFT);
    Button cancel = new Button(AppProperties.getLocalizedString("cancel"));
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
    Button close = new Button(AppProperties.getLocalizedString("close"));

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
            double etaDouble = creator.getETA();
            updateETA(etaDouble);
            double prog = creator.getProgress();

            if (errors > 0) {
              subtitleError.setText(errors + AppProperties.getLocalizedString("CreationModalProcessing.errors"));
            }
            subtitleSuccess.setText(String.format(subtitleFormat, created, size, (int) (prog * 100)));
            progress.setProgress(prog);

            sipName.setText(creator.getSipName());
            sipAction.setText(creator.getAction());

            // stop the timer when all the SIPs have been created
            if ((created + errors) == size) {
              eta.setText(String.format(etaFormatSecond, 0));
              finished();
            }
          }
        });
      }
    };

    timer = new Timer();
    timer.schedule(updater, 0, 600);
  }

  private void updateETA(double etaDouble) {
    if (etaDouble >= 0) {
      int second = (int) ((etaDouble / 1000) % 60);
      int minute = (int) ((etaDouble / (1000 * 60)) % 60);
      int hour = (int) ((etaDouble / (1000 * 60 * 60)) % 24);
      String result;
      if (hour != 0) {
        result = String.format(etaFormatHour, hour, minute, second);
      } else if (minute != 0) {
        result = String.format(etaFormatMinute, minute, second);
      } else
        result = String.format(etaFormatSecond, second);
      eta.setText(result);
    }
  }

  private void finished() {
    timer.cancel();
    setBottom(finishedBox);
  }
}
