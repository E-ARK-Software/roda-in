package org.roda.rodain.ui.inspection.trees;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.roda.rodain.core.Constants;
import org.roda.rodain.core.rules.TreeNode;
import org.roda.rodain.core.shallowSipManager.UriCreator;

import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 17-09-2015.
 */
public class SipContentDirectory extends TreeItem<Object> implements InspectionTreeItem {
  public static final Image folderCollapseImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER));
  public static final Image folderExpandImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_OPEN));
  public static final Image folderCollapseExportImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_EXPORT));
  public static final Image folderExpandExportImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_OPEN_EXPORT));
  private static final Comparator comparator = createComparator();
  // this stores the full path to the file or directory
  private Path fullPath;
  private TreeNode treeNode;
  private TreeItem parent;

  /**
   * Creates a new TreeItem, representing a directory.
   *
   * @param treeNode
   *          The TreeNode that will be associated to the item.
   * @param parent
   *          The item's parent.
   */
  public SipContentDirectory(TreeNode treeNode, TreeItem parent) {
    super(treeNode.getPath());
    this.treeNode = treeNode;
    this.fullPath = treeNode.getPath();
    this.parent = parent;
    if (UriCreator.partOfConfiguration(treeNode.getPath())) {
      this.setGraphic(new ImageView(folderCollapseExportImage));
    } else {
      this.setGraphic(new ImageView(folderCollapseImage));
    }

    final Path name = fullPath.getFileName();
    if (name != null) {
      this.setValue(name.toString());
    } else {
      this.setValue(fullPath.toString());
    }

    this.addEventHandler(TreeItem.branchExpandedEvent(), new EventHandler<TreeModificationEvent<Object>>() {
      @Override
      public void handle(TreeModificationEvent<Object> e) {
        Object sourceObject = e.getSource();
        if (sourceObject instanceof SipContentDirectory) {
          SipContentDirectory source = (SipContentDirectory) sourceObject;
          if (source.isExpanded()) {
            final ImageView iv = (ImageView) source.getGraphic();
            if (UriCreator.partOfConfiguration(source.getPath())) {
              iv.setImage(folderExpandExportImage);
            } else {
              iv.setImage(folderExpandImage);
            }
          }
        }
      }
    });

    this.addEventHandler(TreeItem.branchCollapsedEvent(), new EventHandler<TreeModificationEvent<Object>>() {
      @Override
      public void handle(TreeModificationEvent<Object> e) {
        Object sourceObject = e.getSource();

        if (sourceObject instanceof SipContentDirectory) {
          SipContentDirectory source = (SipContentDirectory) sourceObject;
          if (!source.isExpanded()) {
            final ImageView iv = (ImageView) source.getGraphic();
            if (UriCreator.partOfConfiguration(source.getPath())) {
              iv.setImage(folderCollapseExportImage);
            } else {
              iv.setImage(folderCollapseImage);
            }
          }
        }
      }
    });

  }

  /**
   * @return The TreeNode of this item..
   */
  public TreeNode getTreeNode() {
    return treeNode;
  }

  /**
   * @return The parent item of this item.
   */
  @Override
  public TreeItem getParentDir() {
    return parent;
  }

  /**
   * Skips the directory.
   * <p/>
   * <p>
   * This method removes this node from its parent and adds all its children to
   * the parent, effectively skipping the directory.
   * </p>
   */
  public void skip() {
    SipContentDirectory par = (SipContentDirectory) this.parent;
    TreeNode parentTreeNode = par.getTreeNode();
    // remove this treeNode from the parent
    parentTreeNode.remove(treeNode.getPath());
    // add this treeNode's children to this node's parent
    parentTreeNode.addAll(treeNode.getChildren());
    par.sortChildren();
  }

  /**
   * Flattens the directory.
   * <p/>
   * <p>
   * This method flattens the item's file tree, i.e., moves all it's child nodes
   * to one level.
   * </p>
   */
  public void flatten() {
    treeNode.flatten();
    getChildren().clear();
    for (String path : treeNode.getKeys()) {
      SipContentFile file = new SipContentFile(Paths.get(path), this);
      getChildren().add(file);
    }
    sortChildren();
  }

  /**
   * Sorts the item's children.
   * <p/>
   * <p>
   * The comparator used by this method forces the directories to appear before
   * the files. Between items of the same class the sorting is done comparing the
   * items' values.
   * </p>
   */
  public void sortChildren() {
    ArrayList<TreeItem<Object>> aux = new ArrayList<>(getChildren());
    Collections.sort(aux, comparator);
    getChildren().setAll(aux);

    for (TreeItem ti : getChildren()) {
      if (ti instanceof SipContentDirectory)
        ((SipContentDirectory) ti).sortChildren();
    }
  }

  private static Comparator createComparator() {
    return new Comparator<TreeItem>() {
      @Override
      public int compare(TreeItem o1, TreeItem o2) {
        if (o1.getClass() == o2.getClass()) { // sort items of the same class by
          // value
          String s1 = (String) o1.getValue();
          String s2 = (String) o2.getValue();
          return s1.compareToIgnoreCase(s2);
        }
        // directories must appear first
        if (o1 instanceof SipContentDirectory)
          return -1;
        return 1;
      }
    };
  }

  /**
   * @return The path of this item.
   */
  @Override
  public Path getPath() {
    return this.fullPath;
  }

  /**
   * Sets the parent directory
   *
   * @param t
   *          the new parent directory
   */
  @Override
  public void setParentDir(TreeItem t) {
    parent = t;
  }
}
