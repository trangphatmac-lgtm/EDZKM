package com.zelix.klassmaster.web;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

public final class WebLauncher {
  public static boolean launchIfRequested(String[] args) {
    if (args.length > 0 && args[0].equals("--swing")) {
      com.zelix.ZKM.legacyMain(Arrays.copyOfRange(args, 1, args.length));
      return true;
    }
    if (args.length > 0 && !Set.of("--web", "--port", "--no-browser").contains(args[0]))
      return false;
    try {
      int port = 0;
      boolean browser = true;
      for (int i = 0; i < args.length; i++)
        switch (args[i]) {
          case "--web" -> {}
          case "--no-browser" -> browser = false;
          case "--port" -> {
            if (++i == args.length) throw new IllegalArgumentException("Missing port");
            port = Integer.parseInt(args[i]);
            if (port < 0 || port > 65535)
              throw new IllegalArgumentException("Port must be 0..65535");
          }
          default -> throw new IllegalArgumentException("Unknown web option: " + args[i]);
        }
      WebServer server =
          new WebServer(
              port,
              Path.of(
                  System.getProperty(
                      "zkm.web.home",
                      Path.of(System.getProperty("user.home"), ".zkm-workbench").toString())));
      server.start();
      String url = server.url();
      System.out.println("EDZKM Workbench: " + url);
      System.out.println("Swing: java -jar zkm27.jar --swing");
      Runtime.getRuntime().addShutdownHook(new Thread(server::close));
      if (browser) {
        try {
          if (Desktop.isDesktopSupported()
              && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            Desktop.getDesktop().browse(URI.create(url));
          else System.out.println("Open the URL above in your browser.");
        } catch (Exception e) {
          System.err.println("Could not open browser: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      System.err.println("Web UI startup failed: " + e.getMessage());
      System.err.println("Use --swing for the original interface.");
      System.exit(1);
    }
    return true;
  }
}
