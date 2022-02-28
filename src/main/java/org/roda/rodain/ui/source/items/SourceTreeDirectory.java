package org.roda.rodain.ui.source.items;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.roda.rodain.core.Constants;
import org.roda.rodain.core.Constants.PathState;
import org.roda.rodain.core.PathCollection;
import org.roda.rodain.core.rules.filters.IgnoredFilter;
import org.roda.rodain.core.source.representation.SourceDirectory;
import org.roda.rodain.core.source.representation.SourceItem;
import org.roda.rodain.ui.rules.Rule;
import org.roda.rodain.ui.source.ExpandedEventHandler;
import org.roda.rodain.ui.source.FileExplorerPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 17-09-2015.
 */
public class SourceTreeDirectory extends SourceTreeItem {
  private static final Logger LOGGER = LoggerFactory.getLogger(SourceTreeDirectory.class.getName());
  public static final Image folderCollapseImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_COLAPSE));
  public static final Image folderExpandImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_EXPAND));
  public static final Image folderExpandExportImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_OPEN_EXPORT));
  public static final Image folderCollapseExportImage = new Image(
    ClassLoader.getSystemResourceAsStream(Constants.RSC_ICON_FOLDER_EXPORT));
  public static final Comparator<? super TreeItem> comparator = createComparator();

  public boolean expanded = false;
  private SourceDirectory directory;
  private String fullPath;
  private WatchKey watchKey;

  private HashSet<SourceTreeItem> ignored;
  private HashSet<SourceTreeItem> mapped;
  private HashSet<SourceTreeFile> files;

  public SourceTreeDirectory(Path file, SourceDirectory directory, PathState st, SourceTreeDirectory parent) {
    this(file, directory, parent);
    state = st;
    PathCollection.simpleAddPath(Paths.get(fullPath));
  }

  public SourceTreeDirectory(Path file, SourceDirectory directory, SourceTreeDirectory parent) {
    super(file.toString(), parent);
    this.directory = directory;
    this.fullPath = file.toString();
    this.parent = parent;
    state = PathCollection.getState(Paths.get(fullPath));
    ignored = new HashSet<>();
    mapped = new HashSet<>();
    files = new HashSet<>();

    this.getChildren().add(new SourceTreeLoading());

    String value = file.toString();
    if (!fullPath.endsWith(File.separator)) {
      int indexOf = value.lastIndexOf(File.separator);
      if (indexOf > 0) {
        this.setValue(value.substring(indexOf + 1));
      } else {
        this.setValue(value);
      }
    }

    this.addEventHandler(SourceTreeDirectory.branchExpandedEvent(), new ExpandedEventHandler());

    this.addEventHandler(TreeItem.branchCollapsedEvent(), event -> {
      SourceTreeDirectory source = SourceTreeDirectory.class.cast(event.getSource());
      if (!source.isExpanded()) {
        source.expanded = false;
      }
    });
  }

  public SourceTreeDirectory() {
    super("", null);
    fullPath = "";
    directory = null;
    state = PathState.NORMAL;
    ignored = new HashSet<>();
    mapped = new HashSet<>();
    files = new HashSet<>();
  }

  /**
   * Creates a task to hide all this item's mapped items. The task is needed to
   * prevent the UI thread from hanging due to the computations.
   * <p/>
   * First, it removes all the children with the MAPPED state and adds them to the
   * mapped set, so that they can be shown at a later date. If a child is a
   * directory, this method is called in that item. Finally, clears the children
   * and adds the new list of items.
   *
   * @see #showMapped()
   */
  public synchronized void hideMapped() {
    final ArrayList<TreeItem<String>> newChildren = new ArrayList<>(getChildren());
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        Set<TreeItem> toRemove = new HashSet<>();
        for (TreeItem sti : newChildren) {
          SourceTreeItem item = (SourceTreeItem) sti;
          if (item.getState() == PathState.MAPPED || item instanceof SourceTreeLoadMore) {
            mapped.add(item);
            toRemove.add(sti);
          }
          if (item instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) item).hideMapped();
        }
        newChildren.removeAll(toRemove);
        return null;
      }
    };
    task.setOnSucceeded(event -> getChildren().setAll(newChildren));

    new Thread(task).start();
  }

  /**
   * Creates a task to show all this item's mapped items. The task is needed to
   * prevent the UI thread from hanging due to the computations.
   * <p/>
   * First, it adds all the items in the mapped set, which are the previously
   * hidden items, and clears the set. We need to be careful in this step because
   * if the hiddenFiles flag is true, then we must hide the mapped items that are
   * files. Then makes a call to this method for all its children and hidden
   * ignored items. Finally, clears the children, adds the new list of items, and
   * sorts them.
   *
   * @see #sortChildren()
   * @see #hideMapped()
   */
  public synchronized void showMapped() {
    final ArrayList<TreeItem<String>> newChildren = new ArrayList<>(getChildren());
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        for (SourceTreeItem sti : mapped) {
          if (sti instanceof SourceTreeFile && !FileExplorerPane.isShowFiles()) {
            files.add((SourceTreeFile) sti);
          } else
            newChildren.add(sti);
        }
        mapped.clear();
        for (TreeItem sti : newChildren) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showMapped();
        }
        for (SourceTreeItem sti : ignored) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showMapped();
        }
        return null;
      }
    };

    task.setOnSucceeded(event -> {
      getChildren().setAll(newChildren);
      sortChildren();
    });

    new Thread(task).start();
  }

  /**
   * Creates a task to hide all this item's ignored items. The task is needed to
   * prevent the UI thread from hanging due to the computations.
   * <p/>
   * First, it removes all the children with the IGNORED state and adds them to
   * the ignored set, so that they can be shown at a later date. If a child is a
   * directory, this method is called in that item. Then, calls this method for
   * this item's children directories, that are in the hidden mapped items set.
   * Finally, clears the children and adds the new list of items.
   *
   * @see #showIgnored()
   */
  public synchronized void hideIgnored() {
    final ArrayList<TreeItem<String>> children = new ArrayList<>(getChildren());
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        Set<TreeItem> toRemove = new HashSet<>();
        for (TreeItem sti : children) {
          SourceTreeItem item = (SourceTreeItem) sti;
          if (item.getState() == PathState.IGNORED || sti instanceof SourceTreeLoadMore) {
            ignored.add(item);
            toRemove.add(sti);
          }
          if (item instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) item).hideIgnored();
        }
        children.removeAll(toRemove);
        for (SourceTreeItem item : mapped) {
          if (item instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) item).hideIgnored();
        }
        return null;
      }
    };
    task.setOnSucceeded(event -> getChildren().setAll(children));

    new Thread(task).start();
  }

  /**
   * Creates a task to show all this item's ignored items. The task is needed to
   * prevent the UI thread from hanging due to the computations.
   * <p/>
   * First, it adds all the items in the ignored set, which are the previously
   * hidden items, and clears the set. We need to be careful in this step because
   * if the hiddenFiles flag is true, then we must hide the ignored items that are
   * files. Then makes a call to this method for all its children and hidden
   * mapped items. Finally, clears the children, adds the new list of items, and
   * sorts them.
   *
   * @see #sortChildren()
   * @see #hideIgnored()
   */
  public synchronized void showIgnored() {
    final ArrayList<TreeItem<String>> newChildren = new ArrayList<>(getChildren());
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        for (SourceTreeItem sti : ignored) {
          if (sti instanceof SourceTreeFile && !FileExplorerPane.isShowFiles()) {
            files.add((SourceTreeFile) sti);
          } else
            newChildren.add(sti);
        }
        ignored.clear();
        for (TreeItem sti : newChildren) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showIgnored();
        }
        for (SourceTreeItem sti : mapped) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showIgnored();
        }
        return null;
      }
    };
    task.setOnSucceeded(event -> {
      getChildren().setAll(newChildren);
      sortChildren();
    });
    new Thread(task).start();
  }

  /**
   * Creates a task to hide all this item's file items. The task is needed to
   * prevent the UI thread from hanging due to the computations.
   * <p/>
   * First, it removes all the children that are a file and adds them to the files
   * set, so that they can be shown at a later date. If a child is a directory,
   * this method is called in that item. Finally, clears the children and adds the
   * new list of items.
   *
   * @see #showFiles() ()
   */
  public synchronized void hideFiles() {
    final ArrayList<TreeItem<String>> children = new ArrayList<>(getChildren());
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        Set<TreeItem> toRemove = new HashSet<>();
        for (TreeItem sti : children) {
          if (sti instanceof SourceTreeFile) {
            files.add((SourceTreeFile) sti);
            toRemove.add(sti);
          } else {
            SourceTreeItem item = (SourceTreeItem) sti;
            if (item instanceof SourceTreeDirectory)
              ((SourceTreeDirectory) item).hideFiles();
          }
        }
        children.removeAll(toRemove);
        return null;
      }
    };
    task.setOnSucceeded(event -> getChildren().setAll(children));
    new Thread(task).start();
  }

  /**
   * Creates a task to show all this item's file items. The task is needed to
   * prevent the UI thread from hanging due to the computations.
   * <p/>
   * First, it adds all the items in the files set, which are the previously
   * hidden items, and clears the set. Then makes a call to this method for all
   * its children and hidden ignored/mapped items. Finally, clears the children,
   * adds the new list of items, and sorts them.
   *
   * @see #sortChildren()
   * @see #hideFiles() ()
   */
  public synchronized void showFiles() {
    final ArrayList<TreeItem<String>> newChildren = new ArrayList<>(getChildren());
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        for (SourceTreeItem sti : files) {
          newChildren.add(sti);
        }
        files.clear();
        for (TreeItem sti : newChildren) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showFiles();
        }
        for (SourceTreeItem sti : ignored) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showFiles();
        }
        for (SourceTreeItem sti : mapped) {
          if (sti instanceof SourceTreeDirectory)
            ((SourceTreeDirectory) sti).showFiles();
        }
        return null;
      }
    };
    task.setOnSucceeded(event -> {
      getChildren().setAll(newChildren);
      sortChildren();
    });
    new Thread(task).start();
  }

  /**
   * @return The set of the ignored items in the directory.
   */
  public synchronized Set<String> getIgnored() {
    Set<String> result = new HashSet<>();
    // we need to include the items that are being shown and the hidden
    for (SourceTreeItem sti : ignored) {
      result.add(sti.getPath());
    }
    for (TreeItem sti : getChildren()) {
      SourceTreeItem item = (SourceTreeItem) sti;
      if (item instanceof SourceTreeDirectory)
        result.addAll(((SourceTreeDirectory) item).getIgnored());

      if (item.getState() == PathState.IGNORED)
        result.add(item.getPath());
    }
    return result;
  }

  /**
   * @return The set of the mapped items in the directory.
   */
  public synchronized Set<String> getMapped() {
    Set<String> result = new HashSet<>();
    // we need to include the items that are being shown and the hidden
    for (SourceTreeItem sti : mapped) {
      result.add(sti.getPath());
    }
    for (TreeItem sti : getChildren()) {
      SourceTreeItem item = (SourceTreeItem) sti;
      if (item instanceof SourceTreeDirectory)
        result.addAll(((SourceTreeDirectory) item).getMapped());

      if (item.getState() == PathState.MAPPED)
        result.add(item.getPath());
    }
    return result;
  }

  private static Comparator createComparator() {
    return new Comparator<TreeItem>() {
      @Override
      public int compare(TreeItem o1, TreeItem o2) {
        if (o1.getClass() == o2.getClass()) { // sort items of the same class by
          // value
          String s1 = (String) o1.getValue();
          String s2 = (String) o2.getValue();
          if (s1 != null && s2 != null)
            return s1.compareToIgnoreCase(s2);
        }
        // directories must appear first
        if (o1 instanceof SourceTreeDirectory)
          return -1;
        // "Load More..." item should be at the bottom
        if (o2 instanceof SourceTreeLoadMore)
          return -1;
        return 1;
      }
    };
  }

  /**
   * Sorts the children array
   */
  public void sortChildren() {
    ArrayList<TreeItem<String>> aux = new ArrayList<>(getChildren());
    Collections.sort(aux, comparator);
    getChildren().setAll(aux);
  }

  /**
   * @return The path of the directory
   */
  @Override
  public String getPath() {
    return this.fullPath;
  }

  /**
   * Sets the state of the directory and forces an update of the item
   *
   * @param st
   *          The new state
   */
  @Override
  public void setState(PathState st) {
    if (state != st) {
      state = st;
    }
    forceUpdate();
  }

  /**
   * Sets the directory as ignored and forces an update of the item
   */
  @Override
  public void addIgnore() {
    if (state == PathState.NORMAL) {
      state = PathState.IGNORED;
      PathCollection.addPath(Paths.get(fullPath), state);
    }
    forceUpdate();
  }

  /**
   * Sets the directory as mapped
   */
  @Override
  public void addMapping(Rule r) {
    if (state == PathState.NORMAL) {
      state = PathState.MAPPED;
    }
  }

  /**
   * Sets the directory as normal (if it was ignored)
   */
  @Override
  public void removeIgnore() {
    if (state == PathState.IGNORED) {
      state = PathState.NORMAL;
      PathCollection.addPath(Paths.get(fullPath), state);
    }
  }

  /**
   * @return The SourceDirectory object associated to the item
   */
  public SourceDirectory getDirectory() {
    return directory;
  }

  /**
   * Creates a task to load the items to a temporary collection, otherwise the UI
   * will hang while accessing the disk. Then, sets the new collection as the
   * item's children.
   */
  public synchronized void loadMore() {
    final ArrayList<TreeItem<String>> children = new ArrayList<>(getChildren());
    addToWatcher();

    // Remove "loading" items
    List<Object> toRemove = children.stream()
      .filter(p -> p instanceof SourceTreeLoading || p instanceof SourceTreeLoadMore).collect(Collectors.toList());
    children.removeAll(toRemove);

    // First we access the disk and save the loaded items to a temporary
    // collection
    Task<Integer> task = new Task<Integer>() {
      @Override
      protected Integer call() throws Exception {
        SortedMap<String, SourceItem> loaded = getDirectory().loadMore();
        long startTime = System.currentTimeMillis();

        if (!loaded.isEmpty()) {
          // Add new items
          for (String sourceItem : loaded.keySet()) {
            addChild(children, sourceItem);
          }
          // check if there's more files to load
          if (directory.isStreamOpen())
            children.add(new SourceTreeLoadMore());
        }
        Collections.sort(children, comparator);
        LOGGER.debug("Done adding more child (nr: {} -> millis: {})", loaded.size(),
          (System.currentTimeMillis() - startTime));
        return loaded.size();
      }
    };

    // After everything is loaded, we add all the items to the TreeView at once.
    task.setOnSucceeded(event ->
    // Set the children
    getChildren().setAll(children));

    new Thread(task).start();
  }

  private void addChild(List children, String sourceItem) {
    Path sourceItemPath = Paths.get(sourceItem);

    PathState newState = PathCollection.getState(sourceItemPath);
    if (IgnoredFilter.isIgnored(sourceItemPath)) {
      newState = PathState.IGNORED;
    }

    SourceTreeItem item;
    if (Files.isDirectory(sourceItemPath)) {
      if (newState != PathState.IGNORED) {
        item = new SourceTreeDirectory(sourceItemPath, directory.getChildDirectory(sourceItemPath), newState, this);
      } else {
        item = null;
      }
    } else {
      item = new SourceTreeFile(sourceItemPath, newState, this);
    }

    if (item != null) {
      switch (newState) {
        case IGNORED:
          addChildIgnored(children, item);
          break;
        case MAPPED:
          addChildMapped(children, item);
          break;
        case NORMAL:
          if (item instanceof SourceTreeFile)
            if (FileExplorerPane.isShowFiles())
              children.add(item);
            else
              files.add((SourceTreeFile) item);
          else
            children.add(item);
          break;
        default:
      }
      PathCollection.addItem(item);
    }
  }

  private void addChildIgnored(List children, SourceTreeItem item) {
    if (FileExplorerPane.isShowIgnored()) {
      if (item instanceof SourceTreeFile && !FileExplorerPane.isShowFiles()) {
        files.add((SourceTreeFile) item);
      } else
        children.add(item);
    } else
      ignored.add(item);
  }

  private void addChildMapped(List children, SourceTreeItem item) {
    if (FileExplorerPane.isShowMapped()) {
      if (item instanceof SourceTreeFile && !FileExplorerPane.isShowFiles()) {
        files.add((SourceTreeFile) item);
      } else
        children.add(item);
    } else
      mapped.add(item);
  }

  public synchronized void removeChild(SourceTreeItem item) {
    getChildren().remove(item);
    mapped.remove(item);
    ignored.remove(item);
    files.remove(item);
  }

  /**
   * Moves the children with the wrong state to the correct collection.
   *
   * <p>
   * The normal items in the mapped collection are moved to the children
   * collection.
   * </p>
   *
   * <p>
   * If the mapped items are hidden, moves the mapped items in the children
   * collection to the mapped collection.
   * </p>
   * <p>
   * If the ignored items are hidden, moves the ignored items in the children
   * collection to the ignored collection.
   * </p>
   */
  public synchronized void moveChildrenWrongState() {
    Platform.runLater(() -> {
      Set<SourceTreeItem> toRemove = new HashSet<>();
      boolean modified = false;
      // Move NORMAL items from the mapped set
      for (SourceTreeItem sti : mapped) {
        if (sti.getState() == PathState.NORMAL) {
          toRemove.add(sti);
          getChildren().add(sti);
        }
      }
      mapped.removeAll(toRemove);
      // Move NORMAL items from the ignored set
      for (SourceTreeItem sti : ignored) {
        if (sti.getState() == PathState.NORMAL) {
          toRemove.add(sti);
          getChildren().add(sti);
        }
      }
      ignored.removeAll(toRemove);

      if (!toRemove.isEmpty())
        modified = true;

      if (!FileExplorerPane.isShowMapped()) {
        toRemove = new HashSet<>();
        for (TreeItem ti : getChildren()) {
          SourceTreeItem sti = (SourceTreeItem) ti;
          if (sti.getState() == PathState.MAPPED || sti instanceof SourceTreeLoadMore) {
            toRemove.add(sti);
            mapped.add(sti);
          }
        }
        getChildren().removeAll(toRemove);
      }
      if (!FileExplorerPane.isShowIgnored()) {
        toRemove = new HashSet<>();
        for (TreeItem ti : getChildren()) {
          SourceTreeItem sti = (SourceTreeItem) ti;
          if (sti.getState() == PathState.IGNORED || sti instanceof SourceTreeLoadMore) {
            toRemove.add(sti);
            ignored.add(sti);
          }
        }
        getChildren().removeAll(toRemove);
      }
      if (!toRemove.isEmpty() || modified)
        sortChildren();
    });
  }

  private void addToWatcher() {
    // 20170308 hsilva: disabled watchservice
    // try {
    // if (watchKey == null) {
    // watchKey = directory.getPath().register(FileExplorerPane.watcher,
    // ENTRY_CREATE, ENTRY_DELETE);
    // }
    // } catch (IOException e) {
    // LOGGER.warn("Can't register path to watcher. Will be unable to update the
    // directory: " + directory.getPath(), e);
    // }
  }
}
