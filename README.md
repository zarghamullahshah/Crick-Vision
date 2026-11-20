# Crick-Vision 🏏

A fully **on-device** Flutter app that turns any smartphone into a real-time AI cricket umpire. Using a single camera, **YOLOv8n** and **MediaPipe**, it automatically detects **No-Balls, Wides, Fours and Sixes** at **15–25 FPS** on mid-range Android phones — with **zero cloud dependency**.

Supports **Test, ODI, T20, T10** and recreational cricket.

## Highlights

- 📱 **100% on-device inference** — no server, no internet needed during a match
- ⚡ **Real-time detection** at 15–25 FPS on mid-range Android hardware
- 🎯 Detects No-Balls, Wides, Fours and Sixes from a single camera feed
- 🏆 Match formats: Test, ODI, T20, T10 and casual/recreational games

## Tech Stack

- **Flutter** (Android, Kotlin platform layer)
- **YOLOv8n** — object detection
- **MediaPipe** — on-device ML pipeline

## Getting Started

```bash
git clone https://github.com/zarghamullahshah/Crick-Vision.git
cd Crick-Vision
flutter pub get
flutter run
```

Copy `.env.example` to `.env` and fill in any required values before running.

## Author

**Zargham Ullah Shah** — Full Stack Web Developer | Salesforce CRM Specialist
GitHub: [@zarghamullahshah](https://github.com/zarghamullahshah)
