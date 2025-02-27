package edu.wpi.first.shuffleboard.app;

import edu.wpi.first.shuffleboard.api.data.IncompatibleSourceException;
import edu.wpi.first.shuffleboard.api.prefs.Group;
import edu.wpi.first.shuffleboard.api.sources.DataSource;
import edu.wpi.first.shuffleboard.api.tab.model.ComponentModel;
import edu.wpi.first.shuffleboard.api.widget.Widget;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import org.controlsfx.glyphfont.FontAwesome;

public final class MockWidget implements Widget {

  protected ComponentModel model = null;
  protected double opacity = 1.0;

  @Override
  public List<Group> getSettings() {
    return ImmutableList.of();
  }

  @Override
  public Pane getView() {
    return new Pane();
  }

  @Override
  public Property<String> titleProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Property<FontAwesome.Glyph> glyphProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public BooleanProperty showGlyphProperty() {
    return new SimpleBooleanProperty();
  }

  @Override
  public String getName() {
    return "Mock Widget";
  }

  @Override
  public void addSource(DataSource source) throws IncompatibleSourceException {
    //NOP
  }

  @Override
  public ObservableList<DataSource> getSources() {
    return FXCollections.emptyObservableList();
  }

  @Override
  public boolean hasModel() {
    return model != null;
  }

  @Override
  public ComponentModel getModel() {
    return model;
  }

  @Override
  public void setModel(ComponentModel model) {
    this.model = model;
  }

}
