package com.radar;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import com.fazecast.jSerialComm.*;

/**
 * Ultrasonic Radar System with HC-05 Bluetooth Support
 * Using jSerialComm (modern, no native DLL required)
 */

public class UltrasonicRadar extends JPanel implements Runnable {
    private static final String FONT = "Courier New";
    private static final int LINE = 1;
    private static final int SWEEP = 5;
    // Configuration Constants
    private static final int SCREEN_WIDTH = 1200;
    private static final int SCREEN_HEIGHT = 700;
    private static final int BAUD_RATE = 9600;
    private static final int MAX_DISTANCE = 120;
    private static final int RECONNECT_INTERVAL = 5000;

    // Colors
    private static final Color RADAR_GREEN = new Color(98, 245, 31);
    private static final Color SCAN_GREEN = new Color(30, 250, 60);
    private static final Color OBJECT_RED = new Color(255, 10, 10);
    private static final Color BG_BLACK = new Color(0, 0, 0);
    private static final Color FADE_BLACK = new Color(0, 0, 0, 6);

    // Serial Communication
    private SerialPort serialPort;
    private BufferedReader reader;
    private boolean isConnected = false;
    private long lastReconnectAttempt = 0;
    private String detectedBluetoothPort = null;

    // Data Variables
    private int currentAngle = 0;
    private int currentDistance = 0;
    private boolean dataReceived = false;
    private StringBuilder dataBuffer = new StringBuilder();
    private long lastDataTime = 0;
    private static final long DATA_TIMEOUT = 1000; // 1 second timeout

    // Graphics
    private Image offscreenImage;
    private Graphics2D offscreenGraphics;
    private Thread animationThread;
    private Thread serialThread;
    private boolean running = true;

    public UltrasonicRadar() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(BG_BLACK);
        setDoubleBuffered(true);

        offscreenImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        offscreenGraphics = (Graphics2D) offscreenImage.getGraphics();
        offscreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        connectBluetooth();

        animationThread = new Thread(this);
        animationThread.start();

        startSerialReader();
    }

    private void connectBluetooth() {
        try {
            System.out.println("\n=================================");
            System.out.println("Searching for HC-05 Bluetooth...");
            System.out.println("Available Serial Ports:");

            SerialPort[] ports = SerialPort.getCommPorts();

            // Try to find and connect to Bluetooth port
            for (SerialPort port : ports) {
                String portName = port.getSystemPortName();
                String description = port.getDescriptivePortName().toLowerCase();

                System.out.println("  - " + portName + " (" + port.getDescriptivePortName() + ")");

                // Look for Bluetooth serial ports (Standard Serial over Bluetooth)
                if (description.contains("bluetooth") && description.contains("serial")) {
                    System.out.println("    -> Attempting to connect to Bluetooth port: " + portName);

                    if (tryConnectToPort(port)) {
                        detectedBluetoothPort = portName;
                        System.out.println("    -> SUCCESS! Connected to HC-05 on " + portName);
                        System.out.println("=================================\n");
                        return;
                    } else {
                        System.out.println("    -> Failed to connect to " + portName);
                    }
                }
            }

            // If no Bluetooth port worked, try all available ports
            System.out.println("\nNo Bluetooth port connected. Trying all available ports...");
            for (SerialPort port : ports) {
                String portName = port.getSystemPortName();
                System.out.println("  Trying " + portName + "...");

                if (tryConnectToPort(port)) {
                    detectedBluetoothPort = portName;
                    System.out.println("    -> SUCCESS! Connected on " + portName);
                    System.out.println("=================================\n");
                    return;
                }
            }

            System.err.println("\nFailed to connect to any port!");
            System.out.println("Please check:");
            System.out.println("  1. HC-05 is paired with your PC");
            System.out.println("  2. HC-05 Bluetooth is turned on");
            System.out.println("  3. Arduino is powered and running");
            System.out.println("  4. Close any other apps using the port (Arduino IDE Serial Monitor)");
            System.out.println("=================================\n");

        } catch (Exception e) {
            isConnected = false;
            System.err.println("Connection Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean tryConnectToPort(SerialPort port) {
        try {
            // Close if already open
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                Thread.sleep(500);
            }

            serialPort = port;

            // Configure port with longer timeout for Bluetooth
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    1000, // 1 second read timeout
                    0);

            // Try to open
            if (serialPort.openPort()) {
                Thread.sleep(1000); // Wait for port to stabilize

                serialPort.setComPortParameters(BAUD_RATE, 8, 1, SerialPort.NO_PARITY);
                serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

                // Clear any old data
                serialPort.getInputStream().skip(serialPort.getInputStream().available());

                // Create reader
                reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));

                // Test if data is coming
                Thread.sleep(500);
                if (reader.ready() || testConnection()) {
                    isConnected = true;
                    return true;
                }
            }

            // If we got here, connection failed
            if (serialPort.isOpen()) {
                serialPort.closePort();
            }

        } catch (Exception e) {
            System.err.println("    Error: " + e.getMessage());
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
        }

        return false;
    }

    private boolean testConnection() {
        try {
            // Wait up to 3 seconds for data
            for (int i = 0; i < 30; i++) {
                if (reader.ready()) {
                    System.out.println("    Data detected on port!");
                    return true;
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private void startSerialReader() {
        serialThread = new Thread(() -> {
            while (running) {
                if (isConnected && serialPort != null && serialPort.isOpen()) {
                    try {
                        if (reader.ready()) {
                            int c = reader.read();
                            if (c == -1) {
                                isConnected = false;
                                System.err.println("Connection lost!");
                                continue;
                            }

                            char ch = (char) c;

                            if (ch == '.') {
                                parseData(dataBuffer.toString());
                                dataBuffer.setLength(0);
                            } else {
                                dataBuffer.append(ch);
                            }
                        }
                    } catch (IOException e) {
                        isConnected = false;
                        System.err.println("Error reading serial data: " + e.getMessage());
                    }
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        serialThread.start();
    }

    private void parseData(String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length == 2) {
                currentAngle = Integer.parseInt(parts[0].trim());
                currentDistance = Integer.parseInt(parts[1].trim());
                dataReceived = true;
                lastDataTime = System.currentTimeMillis();
            }
        } catch (NumberFormatException e) {
            // Ignore malformed data
        }
    }

    @Override
    public void run() {
        if (isConnected && lastDataTime > 0 && System.currentTimeMillis() - lastDataTime > DATA_TIMEOUT) {
            isConnected = false;
            System.err.println("Connection lost - no data received!");
        }

        while (running) {
            if (!isConnected && System.currentTimeMillis() - lastReconnectAttempt > RECONNECT_INTERVAL) {
                System.out.println("Attempting reconnection...");
                connectBluetooth();
                lastReconnectAttempt = System.currentTimeMillis();
            }

            repaint();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offscreenGraphics.setColor(FADE_BLACK);
        offscreenGraphics.fillRect(0, 0, SCREEN_WIDTH, (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.065));

        drawRadar(offscreenGraphics);
        drawScanLine(offscreenGraphics);
        drawDetectedObject(offscreenGraphics);
        drawInfoPanel(offscreenGraphics);
        drawConnectionStatus(offscreenGraphics);

        g2d.drawImage(offscreenImage, 0, 0, null);
    }

    private void drawRadar(Graphics2D g2d) {
        g2d.translate(SCREEN_WIDTH / 2, SCREEN_HEIGHT - SCREEN_HEIGHT * 0.074);

        g2d.setColor(RADAR_GREEN);
        g2d.setStroke(new BasicStroke(LINE));

        double[] radii = {
                SCREEN_WIDTH - SCREEN_WIDTH * 0.0625,
                SCREEN_WIDTH - SCREEN_WIDTH * 0.27,
                SCREEN_WIDTH - SCREEN_WIDTH * 0.479,
                SCREEN_WIDTH - SCREEN_WIDTH * 0.687,
                SCREEN_WIDTH - SCREEN_WIDTH * 0.85

        };

        for (double radius : radii) {
            Arc2D arc = new Arc2D.Double(-radius / 2, -radius / 2, radius, radius, 0, 180, Arc2D.OPEN);
            g2d.draw(arc);
        }

        int[] angles = { 0, 15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180 };
        for (int angle : angles) {
            double radAngle = Math.toRadians(angle);
            int x = (int) ((SCREEN_WIDTH / 2) * Math.cos(radAngle));
            int y = (int) (-(SCREEN_WIDTH / 2) * Math.sin(radAngle));
            g2d.drawLine(0, 0, x, y);
        }

        g2d.translate(-SCREEN_WIDTH / 2, -(SCREEN_HEIGHT - SCREEN_HEIGHT * 0.074));
    }

    private void drawScanLine(Graphics2D g2d) {
        if (System.currentTimeMillis() - lastDataTime > DATA_TIMEOUT) {
            return;
        }

        g2d.translate(SCREEN_WIDTH / 2, SCREEN_HEIGHT - SCREEN_HEIGHT * 0.074);

        g2d.setColor(SCAN_GREEN);
        g2d.setStroke(new BasicStroke(SWEEP));

        double lineLength = SCREEN_HEIGHT - SCREEN_HEIGHT * 0.12;
        double radAngle = Math.toRadians(currentAngle);
        int x = (int) (lineLength * Math.cos(radAngle));
        int y = (int) (-lineLength * Math.sin(radAngle));

        g2d.drawLine(0, 0, x, y);

        g2d.translate(-SCREEN_WIDTH / 2, -(SCREEN_HEIGHT - SCREEN_HEIGHT * 0.074));
    }

    private void drawDetectedObject(Graphics2D g2d) {
        if (currentDistance >= MAX_DISTANCE || !dataReceived) {
            return;
        }

        g2d.translate(SCREEN_WIDTH / 2, SCREEN_HEIGHT - SCREEN_HEIGHT * 0.074);

        g2d.setColor(OBJECT_RED);
        g2d.setStroke(new BasicStroke(SWEEP));

        double pixelDistance = currentDistance * ((SCREEN_HEIGHT - SCREEN_HEIGHT * 0.1666) * 0.025);
        double radAngle = Math.toRadians(currentAngle);

        int x1 = (int) (pixelDistance * Math.cos(radAngle));
        int y1 = (int) (-pixelDistance * Math.sin(radAngle));
        int x2 = (int) ((SCREEN_WIDTH - SCREEN_WIDTH * 0.505) * Math.cos(radAngle));
        int y2 = (int) (-(SCREEN_WIDTH - SCREEN_WIDTH * 0.505) * Math.sin(radAngle));

        g2d.drawLine(x1, y1, x2, y2);

        g2d.translate(-SCREEN_WIDTH / 2, -(SCREEN_HEIGHT - SCREEN_HEIGHT * 0.074));
    }

    private void drawInfoPanel(Graphics2D g2d) {
        g2d.setColor(BG_BLACK);
        g2d.fillRect(0, (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0648), SCREEN_WIDTH, SCREEN_HEIGHT);

        g2d.setColor(RADAR_GREEN);
        g2d.setFont(new Font(FONT, Font.PLAIN, 20));

        g2d.drawString("30cm", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.3854),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0833));
        g2d.drawString("60cm", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.281),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0833));
        g2d.drawString("90cm", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.177),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0833));
        g2d.drawString("120cm", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.0729),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0833));

        g2d.setFont(new Font(FONT, Font.BOLD, 24));
        g2d.drawString("Radar System", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.875),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0277));

        double angleRadians = Math.toRadians(currentAngle);
        String angleText = String.format("θ: %d deg (%.2f rad)", currentAngle, angleRadians);
        g2d.drawString(angleText, (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.58),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0277));

        g2d.drawString("s: ", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.26),
                (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0277));

        if (currentDistance < MAX_DISTANCE && dataReceived) {
            double distanceInches = currentDistance * 0.393701;
            String distValue = String.format("%d cm (%.2f in)", currentDistance, distanceInches);
            g2d.drawString(distValue, (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.225),
                    (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0277));
        } else {
            g2d.drawString("Out of Range", (int) (SCREEN_WIDTH - SCREEN_WIDTH * 0.225),
                    (int) (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.0277));
        }

        drawAngleLabels(g2d);
    }

    private void drawAngleLabels(Graphics2D g2d) {
        g2d.setFont(new Font(FONT, Font.PLAIN, 20));
        g2d.setColor(new Color(98, 245, 60));

        drawRotatedLabel(g2d, "30 deg", 30, 60);
        drawRotatedLabel(g2d, "60 deg", 60, 30);
        drawRotatedLabel(g2d, "90 deg", 90, 0);
        drawRotatedLabel(g2d, "120 deg", 120, -30);
        drawRotatedLabel(g2d, "150 deg", 150, -60);
    }

    private void drawRotatedLabel(Graphics2D g2d, String text, int angle, double rotation) {
        AffineTransform old = g2d.getTransform();

        double radAngle = Math.toRadians(angle);
        double x = (SCREEN_WIDTH - SCREEN_WIDTH * 0.5) + SCREEN_WIDTH / 2 * Math.cos(radAngle);
        double y = (SCREEN_HEIGHT - SCREEN_HEIGHT * 0.085) - SCREEN_WIDTH / 2 * Math.sin(radAngle);

        // Measure the text size
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);

        // Move to target location
        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(rotation));

        // Draw centered text
        g2d.drawString(text, -textWidth / 2f, 0);

        // Restore original transform
        g2d.setTransform(old);
    }

    private void drawConnectionStatus(Graphics2D g2d) {
        g2d.setFont(new Font(FONT, Font.BOLD, 18));

        if (isConnected) {
            g2d.setColor(Color.GREEN);
            g2d.drawString("CONNECTED - " + (detectedBluetoothPort != null ? detectedBluetoothPort : ""), 20, 30);

            if (lastDataTime > 0) {
                g2d.setFont(new Font(FONT, Font.PLAIN, 14));
                long timeSinceData = (System.currentTimeMillis() - lastDataTime) / 1000; // Convert to seconds
                if (timeSinceData < 2) {
                    g2d.setColor(new Color(98, 245, 31)); // Green - active
                    g2d.drawString("Data: Active", 20, 55);
                } else {
                    g2d.setColor(Color.ORANGE); // Orange - stale
                    g2d.drawString("Data: " + timeSinceData + "s ago", 20, 55);
                }
            }

        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("DISCONNECTED", 20, 30);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Attempting to reconnect...", 20, 55);
        }
        g2d.setFont(new Font(FONT, Font.BOLD, 16));
        g2d.setColor(new Color(255, 255, 255, 50));

        g2d.drawString("SYECS1 - EDC Project", SCREEN_WIDTH - 220, 30);
        g2d.setFont(new Font(FONT, Font.ITALIC, 14));
        g2d.drawString("by Anvay Mayekar", SCREEN_WIDTH - 220, 50);

    }

    public void cleanup() {
        running = false;

        if (serialThread != null) {
            serialThread.interrupt();
        }

        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }

        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Radar System Shutdown");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ultrasonic Radar System - HC-05 Bluetooth");
            UltrasonicRadar radar = new UltrasonicRadar();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(radar);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    radar.cleanup();
                }
            });
        });
    }
}