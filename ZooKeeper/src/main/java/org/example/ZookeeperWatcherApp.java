package org.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperWatcherApp implements Watcher {
    private static final String ZNODE_A = "/a";
    private static final String ZK_CONNECT = "localhost:2181";
    private final ZooKeeper zooKeeper;
    private Process externalApp = null;
    private final String externalAppCmd;

    public ZookeeperWatcherApp(String externalAppCmd) throws IOException, InterruptedException, KeeperException {
        this.externalAppCmd = externalAppCmd;
        CountDownLatch connectedSignal = new CountDownLatch(1);
        zooKeeper = new ZooKeeper(ZK_CONNECT, 3000, event -> {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        });
        connectedSignal.await();
        System.out.println("Connected to ZooKeeper");
        watchA();
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            String path = event.getPath();
            Event.EventType type = event.getType();
            if (Event.EventType.NodeCreated == type && ZNODE_A.equals(path)) {
                System.out.println("Znode /a has been created!");
                startExternalApp();
                watchChildrenOfA();
                zooKeeper.exists(ZNODE_A, this);
            } else if (Event.EventType.NodeDeleted == type && ZNODE_A.equals(path)) {
                System.out.println("Znode /a has been deleted!");
                stopExternalApp();
                zooKeeper.exists(ZNODE_A, this);
            } else if (Event.EventType.NodeChildrenChanged == type && ZNODE_A.equals(path)) {
                System.out.println("Children of /a have changed");
                showChildrenCount();
                watchChildrenOfA();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void watchA() throws KeeperException, InterruptedException {
        zooKeeper.exists(ZNODE_A, this);
        Stat stat = zooKeeper.exists(ZNODE_A, false);
        if (stat != null) {
            startExternalApp();
            watchChildrenOfA();
        }
    }

    private void watchChildrenOfA() throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(ZNODE_A, false);
        if (stat != null) {
            zooKeeper.getChildren(ZNODE_A, this);
        }
    }

    private void startExternalApp() {
        if (externalApp == null || !externalApp.isAlive()) {
            try {
                System.out.println("Starting external application: " + externalAppCmd);
                externalApp = Runtime.getRuntime().exec(externalAppCmd.split(" "));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopExternalApp() {
        if (externalApp != null && externalApp.isAlive()) {
            System.out.println("Stopping external application...");
            externalApp.destroy();
            externalApp = null;
        }
    }

    private void showChildrenCount() throws KeeperException, InterruptedException {
        List<String> children = zooKeeper.getChildren(ZNODE_A, false);
        int count = children.size();
        System.out.println("Current number of children of /a: " + count);
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Current number of children of /a: " + count,
                "Children of /a", JOptionPane.INFORMATION_MESSAGE));
    }

    public void printTree() throws KeeperException, InterruptedException {
        printTreeRecursive(ZNODE_A, "");
    }

    private void printTreeRecursive(String node, String indent) throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(node, false);
        if (stat == null) {
            System.out.println(indent + node + " (does not exist)");
            return;
        }
        System.out.println(indent + node);
        List<String> children = zooKeeper.getChildren(node, false);
        for (String child : children) {
            printTreeRecursive(node + "/" + child, indent + "  ");
        }
    }

    private void cli() throws Exception {
        while (true) {
            String[] options = {"Display /a tree", "Exit"};
            int opt = JOptionPane.showOptionDialog(null,
                    "What do you want to do?", "Menu",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (opt == 1 || opt == JOptionPane.CLOSED_OPTION) {
                zooKeeper.close();
                if (externalApp != null && externalApp.isAlive()) externalApp.destroy();
                System.exit(0);
            }
            if (opt == 0) {
                printTree();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ZookeeperWatcherApp \"external_app_command\"");
            System.exit(1);
        }
        ZookeeperWatcherApp app = new ZookeeperWatcherApp(args[0]);
        app.cli();
    }
}