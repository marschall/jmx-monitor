package com.github.marschall.jmxmonitor;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JmxMonitor {

  public static void main(String[] args) throws IOException, JMException {
    String url;
    if (args.length > 0) {
      url = args[0];
    } else {
      url = "service:jmx:rmi:///jndi/rmi://servername:9010/jmxrmi";
    }

    JMXServiceURL serviceUrl = new JMXServiceURL(url);
    try (JMXConnector connector = JMXConnectorFactory.connect(serviceUrl)) {
      MBeanServerConnection connection = connector.getMBeanServerConnection();

      ObjectName clusterOne = new ObjectName("com.adobe.granite.replication:type=agent,id=\"replication-publish-1\"");

      ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

      executorService.scheduleAtFixedRate(new PollAttributeValue(connection, clusterOne, "ExpiredCount"), 0, 1, SECONDS);

      try {
        while (!executorService.awaitTermination(1, HOURS)) {
          // wait
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        return;
      }
    }

  }

  static final class PollAttributeValue implements Runnable {

    private volatile long lastValue;

    private final MBeanServerConnection connection;
    private final ObjectName objectName;
    private final String attributeName;

    PollAttributeValue(MBeanServerConnection connection, ObjectName objectName, String attributeName) {
      this.connection = connection;
      this.objectName = objectName;
      this.attributeName = attributeName;
    }

    @Override
    public void run() {
      long currentValue;
      try {
        currentValue = (Long) connection.getAttribute(this.objectName, this.attributeName);
      } catch (JMException | IOException e) {
        e.printStackTrace();
        return;
      }
      long difference = currentValue - this.lastValue;
      System.out.println(difference);
      this.lastValue = currentValue;

    }

  }

}
