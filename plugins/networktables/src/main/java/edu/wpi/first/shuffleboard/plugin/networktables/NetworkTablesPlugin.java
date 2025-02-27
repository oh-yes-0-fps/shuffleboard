package edu.wpi.first.shuffleboard.plugin.networktables;

import edu.wpi.first.shuffleboard.api.DashboardMode;
import edu.wpi.first.shuffleboard.api.data.DataType;
import edu.wpi.first.shuffleboard.api.data.DataTypes;
import edu.wpi.first.shuffleboard.api.plugin.Description;
import edu.wpi.first.shuffleboard.api.plugin.Plugin;
import edu.wpi.first.shuffleboard.api.prefs.FlushableProperty;
import edu.wpi.first.shuffleboard.api.prefs.Group;
import edu.wpi.first.shuffleboard.api.prefs.Setting;
import edu.wpi.first.shuffleboard.api.sources.SourceType;
import edu.wpi.first.shuffleboard.api.sources.recording.Recorder;
import edu.wpi.first.shuffleboard.api.tab.model.TabStructure;
import edu.wpi.first.shuffleboard.api.util.PreferencesUtils;
import edu.wpi.first.shuffleboard.api.widget.ComponentType;
import edu.wpi.first.shuffleboard.api.widget.Components;
import edu.wpi.first.shuffleboard.api.widget.WidgetType;
import edu.wpi.first.shuffleboard.plugin.networktables.sources.NetworkTableSourceType;
import edu.wpi.first.shuffleboard.plugin.networktables.util.NetworkTableUtils;
import edu.wpi.first.util.CombinedRuntimeLoader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;

@Description(
    group = "edu.wpi.first.shuffleboard",
    name = "NetworkTables",
    version = "2.3.1",
    summary = "Provides sources and widgets for NetworkTables"
)
public class NetworkTablesPlugin extends Plugin {

  private int recorderUid = -1;
  private static final Logger log = Logger.getLogger(NetworkTablesPlugin.class.getName());
  private final NetworkTableInstance inst;
  private final Preferences preferences = Preferences.userNodeForPackage(getClass());

  private final StringProperty serverId = new SimpleStringProperty(this, "server", "localhost");
  private final BooleanProperty dynamicMetadata = new SimpleBooleanProperty(this, "dynamic NT widgets", true);
  private final InvalidationListener serverSaver = __ -> PreferencesUtils.save(serverId, preferences);

  private final TabGenerator tabGenerator;
  private final RecorderController recorderController;

  private final ChangeListener<DashboardMode> dashboardModeChangeListener;

  private final HostParser hostParser = new HostParser();

  private final ChangeListener<String> serverChangeListener;

  private static NetworkTableInstance getDefaultInstance() {
    NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
    try {
      var files = CombinedRuntimeLoader.extractLibraries(NetworkTablesPlugin.class,
          "/ResourceInformation-NetworkTables.json");
      CombinedRuntimeLoader.loadLibrary("ntcorejni", files);
    } catch (IOException ex) {
      log.log(Level.SEVERE, "Failed to load NT Core Libraries", ex);
    }

    return NetworkTableInstance.getDefault();
  }

  /**
   * Constructs the NetworkTables plugin.
   */
  public NetworkTablesPlugin() {
    this(getDefaultInstance());
  }

  /**
   * Constructs the NetworkTables plugin.
   */
  public NetworkTablesPlugin(NetworkTableInstance inst) {
    this.inst = inst;
    tabGenerator = new TabGenerator(inst, Components.getDefault(), dynamicMetadata);
    recorderController = RecorderController.createWithDefaultEntries(inst);

    NetworkTableSourceType.setInstance(new NetworkTableSourceType(this));
    inst.startClient4("shuffleboard");
    inst.startDSClient();

    dashboardModeChangeListener = (__, old, mode) -> {
      if (mode == DashboardMode.PLAYBACK) {
        this.recorderController.stop();
      } else {
        this.recorderController.start();
      }
    };

    serverChangeListener = (observable, oldValue, newValue) -> {
      var hostInfoOpt = hostParser.parse(newValue);

      if (hostInfoOpt.isEmpty()) {
        // Invalid input - reset to previous value
        serverId.setValue(oldValue);
        return;
      }

      var hostInfo = hostInfoOpt.get();

      if (hostInfo.getTeam().isPresent()) {
        inst.setServerTeam(hostInfo.getTeam().getAsInt(), hostInfo.getPort());
      } else if (hostInfo.getHost().isEmpty()) {
        inst.setServer("localhost", hostInfo.getPort());
      } else {
        inst.setServer(hostInfo.getHost(), hostInfo.getPort());
      }
      inst.disconnect();
    };
  }

  @Override
  public void onLoad() {
    PreferencesUtils.read(serverId, preferences);
    serverId.addListener(serverSaver);

    // Automatically capture and record changes in network tables
    // This is done here because each key under N subtables would have N+1 copies
    // in the recording (eg "/a/b/c" has 2 tables and 3 copies: "/a", "/a/b", and "/a/b/c")
    // This significantly reduces the size of recording files.
    recorderUid = inst.addListener(
        new String[] {""},
        EnumSet.of(NetworkTableEvent.Kind.kImmediate, NetworkTableEvent.Kind.kValueAll),
        event -> {
          Object value = event.valueData.value.getValue();
          String name = NetworkTableUtils.topicNameForEvent(event);
          DataTypes.getDefault().forJavaType(value.getClass())
              .ifPresent(type -> {
                Recorder.getInstance().record(
                    NetworkTableSourceType.getInstance().toUri(name),
                    type,
                    value
                );
              });
        });

    DashboardMode.currentModeProperty().addListener(dashboardModeChangeListener);
    recorderController.start();
    tabGenerator.start();

    serverChangeListener.changed(null, null, serverId.get());
    serverId.addListener(serverChangeListener);
  }

  @Override
  public void onUnload() {
    DashboardMode.currentModeProperty().removeListener(dashboardModeChangeListener);
    recorderController.stop();
    tabGenerator.stop();
    NetworkTablesJNI.removeListener(recorderUid);
    NetworkTableUtils.shutdown(inst);
    serverId.removeListener(serverSaver);
  }

  @Override
  public List<ComponentType> getComponents() {
    return ImmutableList.of(
        WidgetType.forAnnotatedWidget(NetworkTableTreeWidget.class)
    );
  }

  @Override
  public List<SourceType> getSourceTypes() {
    return ImmutableList.of(
        NetworkTableSourceType.getInstance()
    );
  }

  @Override
  public Map<DataType, ComponentType> getDefaultComponents() {
    return ImmutableMap.of(
        DataTypes.Map, WidgetType.forAnnotatedWidget(NetworkTableTreeWidget.class)
    );
  }

  @Override
  public List<Group> getSettings() {
    return ImmutableList.of(
        Group.of("Connection settings",
            Setting.of("Server",
                "The NetworkTables server to connect to. This can be a team number, IP address, or mDNS URL",
                new FlushableProperty<>(serverId)
            )
        ),
        Group.of("Widget settings",
            Setting.of("Dynamic metadata",
                "Whether to automatically change the appearance of widgets based on the current metadata isntead of at initialization",
                new FlushableProperty<>(dynamicMetadata)
            )
        )
    );
  }

  @Override
  public TabStructure getTabs() {
    return tabGenerator.getStructure();
  }

  public String getServerId() {
    return serverId.get();
  }

  public StringProperty serverIdProperty() {
    return serverId;
  }

  public void setServerId(String serverId) {
    this.serverId.set(serverId);
  }

}
