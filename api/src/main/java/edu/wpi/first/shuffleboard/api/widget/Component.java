package edu.wpi.first.shuffleboard.api.widget;

import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.scene.layout.Pane;
import org.controlsfx.glyphfont.FontAwesome;

import edu.wpi.first.shuffleboard.api.tab.model.ComponentModel;

/**
 * A Component is base interface for any part of the dashboard that can be instantiated by the user. For example, a
 * static image, a camera widget, and a layout that has children in lists could all be components of different types.
 *
 * <p>Components can also be FXML controllers. This allows the component to define its {@link #getView() view} in an
 * FXML file and only have to contain the logic of controlling or manipulating that view. Such components should have
 * an {@link ParametrizedController @ParametrizedController} annotation so that shuffleboard can know where the FXML
 * file is located.
 */
public interface Component extends SettingsHolder {

  /**
   * Gets a JavaFX pane that displays this component.
   */
  Pane getView();

  /**
   * Gets the label for this component.
   */
  Property<String> titleProperty();

  /**
   * Gets the glyph for this component.
   */
  Property<FontAwesome.Glyph> glyphProperty();

  /**
   * Gets whether the glyph should be shown for this component.
   */
  BooleanProperty showGlyphProperty();

  default String getTitle() {
    return titleProperty().getValue();
  }

  default void setTitle(String title) {
    titleProperty().setValue(title);
  }

  /**
   * All of the components contained by or represented by this one, if any.
   */
  Stream<Component> allComponents();

  /**
   * Gets the name of this widget type. This should be unique among all component types; if not, attempting to register
   * it with the {@link Components component registry} will fail and this component will not be usable. The name is
   * class-intrinsic.
   */
  String getName();

  /**
   * Checks if this component has a model.
   */
  boolean hasModel();

  /**
   * Gets the model for this component,
   * if model is not set returns null
   */
  ComponentModel getModel();

  /**
   * Sets the model for this component.
   */
  void setModel(ComponentModel model);
}
