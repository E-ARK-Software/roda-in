package org.roda.rodain.ui.schema.ui;

import java.util.Observable;
import java.util.Observer;

import org.roda.rodain.core.ConfigurationManager;
import org.roda.rodain.core.Constants;
import org.roda.rodain.core.sip.SipPreview;
import org.roda.rodain.ui.utils.FontAwesomeImageCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 05-10-2015.
 */
public class SipPreviewNode extends TreeItem<String> implements Observer {
  private SipPreview sip;
  private Image iconBlack, iconWhite;

  private boolean blackIconSelected = true;
  private static final Logger LOGGER = LoggerFactory.getLogger(SipPreviewNode.class.getName());

  /**
   * Creates a new SipPreviewNode
   *
   * @param sip
   *          The SipPreview object to be wrapped
   * @param iconBlack
   *          The icon to be used in the SipPreviewNode, with the color black
   * @param iconWhite
   *          The icon to be used in the SipPreviewNode, with the color white
   */
  public SipPreviewNode(SipPreview sip, Image iconBlack, Image iconWhite) {
    super(sip.getTitle());
    this.sip = sip;
    this.iconBlack = iconBlack;
    this.iconWhite = iconWhite;
    setGraphic(new ImageView(iconBlack));
  }

  /**
   * @return The SipPreview object that the SipPreviewNode is wrapping
   */
  public SipPreview getSip() {
    return sip;
  }

  /**
   * @return The SipPreviewNode's icon
   */
  public Image getIcon() {
    if (blackIconSelected) {
      return iconBlack;
    } else
      return iconWhite;
  }

  public Image getIconWhite() {
    return iconWhite;
  }

  public void updateDescriptionLevel(String descLevel) {
    sip.setDescriptionlevel(descLevel);
    try {
      String unicode = ConfigurationManager.getConfig(Constants.CONF_K_SUFFIX_LEVELS_ICON + descLevel);
      if (unicode != null) {
        Platform.runLater(() -> {
          iconBlack = FontAwesomeImageCreator.generate(unicode);
          iconWhite = FontAwesomeImageCreator.generate(unicode, Color.WHITE);
          this.setGraphic(new ImageView(getIcon()));
        });
      } else {
        String unicodeDefault = ConfigurationManager.getConfig(Constants.CONF_K_LEVELS_ICON_DEFAULT);
        if (unicodeDefault != null) {
          Platform.runLater(() -> {
            iconBlack = FontAwesomeImageCreator.generate(unicodeDefault);
            iconWhite = FontAwesomeImageCreator.generate(unicodeDefault, Color.WHITE);
            this.setGraphic(new ImageView(getIcon()));
          });
        }
      }
    } catch (Exception e) {
      // We don't need to process this exception, since it's expected that
      // there
      // will be a lot of them thrown. It could happen because the user
      // still
      // hasn't finished writing the new description level title
      return;
    }
  }

  public void setBlackIconSelected(boolean value) {
    blackIconSelected = value;
  }

  /**
   * @return True if the SipPreview's content has been modified, false otherwise
   * @see SipPreview#isContentModified()
   */
  public boolean isContentModified() {
    return sip.isContentModified();
  }

  /**
   * Forces the redraw of the item
   *
   * @param o
   * @param arg
   */
  @Override
  public void update(Observable o, Object arg) {
    Platform.runLater(() -> {
      String value = getValue();
      setValue("");
      setValue(value); // this forces a redraw of the item
    });
  }
}
