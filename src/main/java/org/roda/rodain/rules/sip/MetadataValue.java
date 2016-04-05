package org.roda.rodain.rules.sip;

import org.roda.rodain.utils.UIPair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 08-02-2016.
 */
public class MetadataValue {
  private String title, id;
  private List<UIPair> fieldOptions;
  private String value;

  public MetadataValue() {
    fieldOptions = new ArrayList<>();
  }

  /**
   * Creates a new MetadataValue object.
   *
   * @param id
   *          The id of the MetadataValue object.
   * @param title
   *          The title of the MetadataValue object.
   * @param value
   *          The value of the MetadataValue object.
   */
  public MetadataValue(String id, String title, String value) {
    this.id = id;
    this.title = title;
    this.value = value;
    this.fieldOptions = new ArrayList<>();
  }

  /**
   * @return The ID of the object.
   */
  public String getId() {
    return id;
  }

  /**
   * @return The title of the object.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the value of the object.
   * 
   * @param value
   *          The new value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * @return The value of the object.
   */
  public String getValue() {
    return value;
  }

  /**
   * @return The field options of the object.
   */
  public List<UIPair> getFieldOptions() {
    return fieldOptions;
  }

  /**
   * Adds a field option to the object. These are used in the "combo" field type
   * to populate the list.
   * 
   * @param option
   *          The new field option
   */
  public void addFieldOption(UIPair option) {
    fieldOptions.add(option);
  }
}
