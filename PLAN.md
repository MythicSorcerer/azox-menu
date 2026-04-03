# Music Feature - Implementation Complete

## Overview
Added music playback functionality with player rating system and sidebar display.

## Features Implemented

### 1. Music Playback
- Plays random 30-second music clips from `assets/` folder
- Randomly selects eligible players (those with music enabled)
- Configurable play interval and minimum player count
- Uses music disc sound for playback

### 2. Rating System
- Players are prompted to rate tracks 1-5 when music plays
- Prompt shows track name and rating options
- Rating expires after 15 seconds
- `/music off` to disable custom music

### 3. Manual Music Control
- `/music on` - Enable custom music
- `/music off` - Disable custom music  
- `/music play` - Play a random track manually
- `/music status` - Check current music status
- Rating numbers (1-5) work as shortcuts when rating prompt is active

### 4. Sidebar Integration
- Shows "Music: [Track Name]" in sidebar
- Automatically updates when track changes
- Configurable via `sidebar.entries.show-music`
- "music" added to default sidebar order

### 5. Default Minecraft Music Disabled
- `DO_INSOMNIA` gamerule set to prevent phantom spawning (which triggers music)

## Files Created/Modified

### New Files
- `config/MusicConfig.java` - Music configuration handling
- `managers/MusicManager.java` - Core music playback logic
- `commands/MusicCommand.java` - `/music` command handler
- `listeners/MusicListener.java` - Player join/quit events

### Modified Files
- `AzoxMenu.java` - Added music manager initialization, disabled default music
- `SidebarConfig.java` - Added `showMusic` config option
- `SidebarManager.java` - Added music entry display
- `config.yml` - Added music configuration section
- `paper-plugin.yml` - Added `/music` command and permission
- `pom.xml` - Fixed resource filtering for binary MP3 files

## Configuration

```yaml
music:
  enabled: true
  min-players: 3          # Minimum online players to start
  clip-duration: 30       # Clip length in seconds
  play-interval: 60       # Seconds between clips
  random-player-chance: 0.3  # Chance to select random player

sidebar:
  entries:
    show-music: true      # Show music in sidebar
    order:                # Sidebar entry order
      - music
      - status
      - ...
```

## Permissions
- `azox.menu` - Control music playback (default: true)
- `azox.admin` - Reload plugin (default: op)
- `azox.news` - Manage news (default: op)
