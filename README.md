# 📡 **Ultrasonic Radar** — Real-Time Object Detection & Visualization System

A sophisticated radar interface built with **Java Swing** that communicates with an **Arduino-based ultrasonic sensor system** via **HC-05 Bluetooth**, providing real-time object detection and visualization with a classic radar sweep animation.

> 🛠️ This project was developed as a **Micro Project** for the subject **Electronic Devices and Circuits (EDC)** at **Shah & Anchor Kutchhi Engineering College (SAKEC)**, Mumbai, as part of the **B.Tech (Electronics & Computer Science)** curriculum.

> ⚠️ **Note:** This project contains minor discrepancies in real-time synchronization between servo movement and GUI rendering, which are being addressed in future iterations.

![Radar Demo](sample/demo.gif)

---

## 📌 Overview

The **Ultrasonic Radar** project combines hardware and software to create a functional object detection system. An **MG995 servo motor** rotates an **HC-SR04 ultrasonic sensor** in a 180° arc, measuring distances to nearby objects. The **Arduino Uno** processes sensor data and transmits it wirelessly via **HC-05 Bluetooth** to a Java application that renders a real-time radar display.

---

## 🔧 Hardware Components

| Component | Model/Type | Purpose |
|-----------|------------|---------|
| **Microcontroller** | Arduino Uno | Controls servo motor and reads ultrasonic sensor data |
| **Servo Motor** | MG995 | Rotates the ultrasonic sensor 180° for scanning |
| **Ultrasonic Sensor** | HC-SR04 | Measures distance to objects (2cm - 400cm range) |
| **Bluetooth Module** | HC-05 | Wireless serial communication with PC |
| **Power Supply** | 5V USB / External | Powers Arduino and peripherals |

### 🔌 Pin Configuration

```
HC-SR04 Ultrasonic Sensor:
  ├─ TRIG → Arduino Pin 8
  ├─ ECHO → Arduino Pin 9
  ├─ VCC  → 5V
  └─ GND  → GND

MG995 Servo Motor:
  ├─ Signal → Arduino Pin 12
  ├─ VCC    → 5V (External recommended for high torque)
  └─ GND    → GND

HC-05 Bluetooth Module:
  ├─ TX  → Arduino Pin 2 (RX via SoftwareSerial)
  ├─ RX  → Arduino Pin 3 (TX via SoftwareSerial)
  ├─ VCC → 5V
  └─ GND → GND
```

---

## 📁 File Structure

```
ultrasonic-radar/
├── sample/
│   └── demo.gif                    # 🎬 Demo animation of radar in action
│
├── ino/
│   └── radar.ino                   # 🤖 Arduino firmware for sensor & servo control
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── radar/
│                   └── UltrasonicRadar.java  # 🖥️ Java GUI application
│
├── target/                         # 📦 Maven build output directory
│
├── pom.xml                         # 🔧 Maven project configuration
└── README.md                       # 📘 Project documentation
```

---

## ⚙️ Features & Capabilities

### ✅ Software Features

| Feature | Description |
|---------|-------------|
| **Real-Time Visualization** | Classic radar sweep animation with object detection overlay |
| **Bluetooth Auto-Detection** | Automatically scans and connects to HC-05 Bluetooth serial ports |
| **Fallback Serial Support** | Falls back to USB serial if Bluetooth is unavailable |
| **Auto-Reconnection** | Attempts to reconnect every 5 seconds on connection loss |
| **Data Timeout Handling** | Stops rendering sweep line 1 second after last data received |
| **Distance Measurement** | Displays distance in both centimeters and inches |
| **Angle Tracking** | Shows current sweep angle in degrees and radians |
| **Connection Status** | Visual indicators for connection state and data freshness |
| **Smooth Animation** | 60 FPS rendering with fade effects for scan trails |

### 🎯 Technical Highlights

- **Intelligent Port Detection**: Prioritizes Bluetooth serial ports, then scans all available COM ports
- **Exception Handling**: Robust error handling for serial I/O, connection loss, and malformed data
- **Thread-Safe Communication**: Separate threads for GUI rendering and serial data reading
- **Data Validation**: Filters out-of-range values and malformed packets
- **Memory Efficient**: Uses double buffering to prevent screen flicker

---

## 🧰 Tools & Technologies

![Java](https://img.shields.io/badge/Java_11-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Arduino](https://img.shields.io/badge/Arduino-00979D?style=for-the-badge&logo=Arduino&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Bluetooth](https://img.shields.io/badge/Bluetooth-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)
![Swing](https://img.shields.io/badge/Java_Swing-007396?style=for-the-badge&logo=java&logoColor=white)

---

## 📚 Libraries & Dependencies

### Java Dependencies (Maven)

```xml
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.10.4</version>
</dependency>
```

- **jSerialComm**: Modern, cross-platform serial communication library (no native DLLs required)

### Arduino Libraries

```cpp
#include <Servo.h>           // Servo motor control
#include <SoftwareSerial.h>  // Bluetooth communication
```

---

## 🚀 Getting Started

### 📋 Prerequisites

- **Java Development Kit (JDK) 11** or higher
- **Apache Maven 3.6+**
- **Arduino IDE** (for uploading firmware)
- **HC-05 Bluetooth** paired with your PC (Windows/Linux)

### 🔌 Hardware Setup

1. **Wire the components** according to the pin configuration above
2. **Pair HC-05** with your computer:
   - Default pairing code: `1234` or `0000`
   - Ensure Bluetooth is turned on
3. **Upload Arduino sketch**:
   ```bash
   # Open ino/radar.ino in Arduino IDE
   # Select Board: Arduino Uno
   # Select Port: Your Arduino's COM port
   # Click Upload
   ```

### 💻 Software Installation

#### 🧩 Clone Repository

```bash
git clone https://github.com/anvaymayekar/ultrasonic-radar.git
cd ultrasonic-radar
```

#### 🔧 Build with Maven

```bash
# Clean previous builds and compile
mvn clean compile
```

#### 🚀 Run Application

```bash
# Execute the radar GUI
mvn exec:java
```

Alternatively, you can build a standalone JAR:

```bash
mvn package
java -jar target/ultrasonic-radar-1.0.0.jar
```

---

## 🔄 How It Works

### 📡 Communication Flow

```
┌─────────────┐        Bluetooth/Serial       ┌──────────────┐
│   Arduino   │  ◄─────────────────────────►  │  Java GUI    │
│    + HC-05  │       "angle,distance."       │   (Swing)    │
└─────────────┘                               └──────────────┘
      │                                             │
      ├─ Controls Servo (0°-180°)                   ├─ Renders Radar Display
      ├─ Reads HC-SR04 Distance                     ├─ Draws Sweep Line
      └─ Transmits: "45,67."                        └─ Highlights Objects
```

### 🔍 Connection Logic

1. **Bluetooth Port Scan**: Searches for ports with "bluetooth" + "serial" in description
2. **Automatic Connection**: Attempts connection with 9600 baud rate
3. **Data Validation Test**: Waits up to 3 seconds for valid data stream
4. **Fallback to All Ports**: If Bluetooth fails, tries all available COM ports sequentially
5. **Auto-Reconnect**: Retries connection every 5 seconds on disconnection

### 🛡️ Exception Handling

- **Connection Loss**: Detects when no data received for >1 second, triggers reconnection
- **Malformed Data**: Filters invalid packets (non-numeric, incomplete)
- **Port Busy**: Handles conflicts with Arduino IDE Serial Monitor
- **Out-of-Range Values**: Clamps distances to 0-400cm, discards impossible readings
- **Thread Safety**: Synchronizes access to shared data between GUI and serial threads

### ⏱️ Data Protocol

```
Format: angle,distance.
Example: 45,67.
         │  │  └─ Delimiter (marks end of packet)
         │  └──── Distance in centimeters
         └─────── Angle in degrees (0-180)
```

---

## 🖼️ GUI Components

### 📊 Display Elements

- **Radar Arcs**: 4 concentric arcs representing 30cm, 60cm, 90cm, 120cm ranges
- **Angle Lines**: Radial lines at 15° intervals (0° to 180°)
- **Sweep Line**: Green animated line showing current sensor angle
- **Object Indicator**: Red line from detected object to perimeter
- **Info Panel**: Displays current angle (deg/rad) and distance (cm/in)
- **Connection Status**: Shows port name and data freshness indicator
- **Credits**: Project and team information

---

## ⚠️ Known Issues & Limitations

1. **Minor Sync Delay**: ~50ms latency between physical servo position and GUI sweep line
2. **Bluetooth Range**: Limited to HC-05 range (~10 meters in open space)
3. **Sensor Blind Spots**: HC-SR04 has minimum range of 2cm
4. **Angle Precision**: Servo movement granularity is 1 degree
5. **Single Object Detection**: Displays only nearest object at each angle

---

## 🔮 Future Enhancements

- Add data logging to CSV file
- Implement object tracking and path prediction
- Multi-object detection support
- Adjustable scan speed and range
- Export radar snapshots as images
- Add sound alerts for close objects
- Web-based interface using WebSockets

---

## 📖 Usage Tips

✅ **For Best Results:**
- Close Arduino IDE Serial Monitor before running
- Ensure HC-05 is paired and powered on
- Use external 5V supply for servo motor (high current draw)
- Place sensor at least 50cm above ground
- Avoid metallic or sound-absorbing surfaces

❌ **Troubleshooting:**
- **"No ports found"**: Check Bluetooth pairing and drivers
- **"Connection timeout"**: Verify baud rate is 9600 on both sides
- **Erratic readings**: Check wiring and sensor placement
- **Servo jitter**: Use external power supply for servo

---


## ⚖️ License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).  
You are free to use, modify, and distribute this software with proper attribution.

---

## 👨‍💻 Author

> **Anvay Mayekar**  
> 🎓 B.Tech in Electronics & Computer Science — SAKEC  
>
> [![GitHub](https://img.shields.io/badge/GitHub-181717.svg?style=for-the-badge&logo=GitHub&logoColor=white)](https://www.github.com/anvaymayekar)
> [![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2.svg?style=for-the-badge&logo=LinkedIn&logoColor=white)](https://in.linkedin.com/in/anvaymayekar)
>[![Gmail](https://img.shields.io/badge/Gmail-D14836.svg?style=for-the-badge&logo=gmail&logoColor=white)](mailto:anvaay@gmail.com)
