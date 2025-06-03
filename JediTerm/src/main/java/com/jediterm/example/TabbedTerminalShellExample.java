package com.jediterm.example;

import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.CursorShape;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jediterm.app.PlatformUtilKt.isWindows;

public class TabbedTerminalShellExample {

  private static @NotNull JediTermWidget createTerminalWidget() {
    JediTermWidget widget = new JediTermWidget(80, 24, new DefaultSettingsProvider());
    widget.getTerminalPanel().setDefaultCursorShape(CursorShape.BLINK_UNDERLINE);
    widget.setTtyConnector(createTtyConnector());
    widget.start();
    return widget;
  }

  private static @NotNull TtyConnector createTtyConnector() {
    try {
      Map<String, String> envs = System.getenv();
      String[] command;
      if (isWindows()) {
        command = new String[]{"cmd.exe"};
      }
      else {
        command = new String[]{"/bin/bash", "--login"};
        envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm-256color");
      }

      PtyProcess process = new PtyProcessBuilder().setCommand(command).setEnvironment(envs).start();
      return new PtyProcessTtyConnector(process, StandardCharsets.UTF_8);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private JFrame frame;
  private JTabbedPane tabbed;
  private List<JediTermWidget> widgets = new ArrayList<>();

  private void createAndShowGUI() {
    frame = new JFrame("JediTerm");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    tabbed = new JTabbedPane();
    frame.setContentPane(tabbed);

    tabbed.addChangeListener(e -> {
      Component selected = tabbed.getSelectedComponent();
      if (selected != null) {
        selected.requestFocusInWindow();
      }
    });

    addTerminalWidget(true);

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        frame.setVisible(false);
        for (JediTermWidget widget : widgets) {
          widget.getTtyConnector().close(); // terminate the current process
        }
      }
    });

    frame.pack();
    frame.setVisible(true);

    InputMap inputMap = tabbed.getInputMap();
    ActionMap actionMap = tabbed.getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "ctrl-t");
    actionMap.put("ctrl-t", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        addTerminalWidget(true);
      }
    });
  }

  private void addTerminalWidget(boolean focus) {
    JediTermWidget widget = createTerminalWidget();
    widgets.add(widget);
    widget.getTerminal().addApplicationTitleListener(title -> {
      int index = tabbed.indexOfComponent(widget);
      tabbed.setTitleAt(index, title);
    });
    tabbed.add(widget);
    widget.addListener(terminalWidget -> {
      widget.close(); // terminate the current process and dispose all allocated resources
      SwingUtilities.invokeLater(() -> {
        tabbed.remove(widget);
        if (tabbed.getTabCount() == 0) {
          System.exit(0);
        }
      });
    });

    widget.getTerminalPanel().addCustomKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        super.keyTyped(e);
        if (e.getKeyCode() == KeyEvent.VK_T && e.isControlDown()) {
          addTerminalWidget(true);
        }
      }
    });

    if (focus) {
      SwingUtilities.invokeLater(() -> {
        tabbed.setSelectedComponent(widget);
        widget.getTerminalPanel().requestFocus();
      });
    }
  }

  public static void main(String[] args) {
    // Create and show this application's GUI in the event-dispatching thread.
    TabbedTerminalShellExample tabbedTerminal = new TabbedTerminalShellExample();
    SwingUtilities.invokeLater(tabbedTerminal::createAndShowGUI);
  }
}
