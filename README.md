# BackupMod

BackupMod is a powerful Minecraft server mod for easy, automatic, and manual backups, with permission management, auto-deletion, and more.

---

## Download

- **Modrinth:** [Not Uploaded]
- **CurseForge:** [www.curseforge.com/minecraft/mc-mods/backup-mod]

---

## 🇬🇧 English README

### Features

- **Create and restore backups** with simple commands
- **Automatic scheduled backups** (configurable per day)
- **Backup limit & auto-delete** of oldest files
- **Upload ZIP from URL (Upload Zip via direct download link. tip google drive doesn't work.)** 
- **Rename backups**
- **Exclude/include folders/files** from backups
- **Language change command en_us, en_gb, de_de, it_it, fr_fr, es_es, pl_pl, more comming soon**
- **Permission system** (only OPs or authorized users)
- **Configurable logging**

---

### Installation

1. Download the mod (`backupmod-x.x.x.jar`)
2. Place it in your server’s `mods` folder
3. Start the server – done!

---

### Commands & Usage

All commands start with `/backup ...`  
**Only OPs or users with permission can use these commands.**

| Command                                  | Description                                                         |
|-------------------------------------------|---------------------------------------------------------------------|
| `/backup help`                           | Show help and list all commands                                     |
| `/backup create`                         | Create a new backup (auto-named)                                    |
| `/backup create <name>`                  | Create a backup with a custom name                                  |
| `/backup restore <zip>`                  | Restore a specific backup (kicks all players, stops server)         |
| `/backup restorelatest`                  | Restore the newest backup                                           |
| `/backup list`                           | List all available backups                                          |
| `/backup delete <zip>`                   | Delete a backup file                                                |
| `/backup upload <url> <name>`            | Download a ZIP from URL and save as a backup                        |
| `/backup rename <zip> <newname>`         | Rename a backup (only the part before first dash changes)           |
| `/backup autobackup <num>`               | Enable automatic backups (e.g. `5` means 5/day)                     |
| `/backup autobackup off`                 | Disable automatic backups                                           |
| `/backup autobackup request`             | Show next scheduled backup timer                                    |
| `/backup maxbackup <num>`                | Set maximum backups to keep (oldest will be deleted)                |
| `/backup maxbackup off`                  | Remove backup limit                                                 |
| `/backup maxbackup request`              | Show current backup limit                                           |
| `/backup lang <lang>`                    | Change language (e.g. `en_us`, `de_de`, `fr_fr`, ...)              |
| `/backup exclude <file/folder>`          | Exclude a file or folder from backup                                |
| `/backup enclude <file/folder>`          | Re-include it in backup                                             |
| `/backup permissions add <player>`       | Give a player permission to use backups                             |
| `/backup permissions remove <player>`    | Remove backup permission                                            |
| `/backup permissions list`               | List all players with backup permission                             |
| `/backup log normallog on/off`           | Toggle normal log for all players                                   |
| `/backup log permlog on/off`             | Toggle detailed log for OPs/perm users                              |
| `/backup debug ...`                      | Debug/test commands (see `/backup help`)                            |

---

### Automatic Backups

- `/backup autobackup <num>` enables auto-backups (e.g. `5` for 5/day)
- `/backup autobackup request` shows time to next backup
- `/backup maxbackup <num>` sets the max number of backups to keep

---

### Permissions

- Only OPs or users with permission (see `/backup permissions ...`) can use commands.
- Use `/backup permissions add <player>` and `/backup permissions remove <player>` to manage.

---

### Language

- Default is English (`en_us`)
- Change with `/backup lang <lang> (en_us, en_gb, de_de, it_it, fr_fr, es_es, pl_pl, more comming soon)`

---

### Exclude/Include

- `/backup exclude <name>` to exclude files/folders
- `/backup enclude <name>` to re-include them

---

### Logging

- `/backup log normallog on/off` for normal player logging
- `/backup log permlog on/off` for detailed OP/permission user logs

---

### Where Are Backups Stored?

- All backups: `backups/` folder in the server directory
- Temporary/system data: `backups/.temp` (**do not delete!**)
- Logs: `backups/logs`
- Config: `backups/backup-config.json`

---

### Reporting Issues & Support

**Please report issues, bugs or feature requests on Modrinth or CurseForge!**  
Do not use GitHub issues for bug reports.

---

**Enjoy and keep your worlds safe!**  
_Made with ♥ by Mcjunky33_

---

---

## 🇩🇪 Deutsche README

### Features

- **Backups erstellen und wiederherstellen** per Command
- **Automatische Backups** (mehrmals am Tag möglich)
- **Backup-Limit & automatisches Löschen** der ältesten Sicherungen
- **ZIP per URL herunterladen (upload via directdownload link. tipp google drive funktioniert nicht.)** 
- **Umbenennen von Backups**
- **Dateien/Ordner ausschließen/inkludieren**
- **Sprache umschaltbar en_us, en_gb, de_de, it_it, fr_fr, es_es, pl_pl, more comming soon**
- **Rechteverwaltung für Backup-Kommandos**
- **Log ein- und ausschaltbar**

---

### Installation

1. Mod herunterladen (`backupmod-x.x.x.jar`)
2. In den `mods`-Ordner des Servers kopieren
3. Server starten – fertig!

---

### Kommandos & Erklärung

Alle Befehle starten mit `/backup ...`  
**Nur OPs oder berechtigte Spieler dürfen die Kommandos nutzen!**

| Kommando                               | Erklärung                                                          |
|-----------------------------------------|--------------------------------------------------------------------|
| `/backup help`                         | Listet alle Backup-Kommandos und erklärt sie                       |
| `/backup create`                       | Erstellt ein neues Backup (automatischer Name)                     |
| `/backup create <name>`                | Erstellt ein Backup mit eigenem Namen                              |
| `/backup restore <zip>`                | Stellt das angegebene Backup wieder her (Spieler werden gekickt)   |
| `/backup restorelatest`                | Stellt das neueste Backup wieder her                               |
| `/backup list`                         | Zeigt alle Backups an                                              |
| `/backup delete <zip>`                 | Löscht ein Backup-ZIP                                              |
| `/backup upload <url> <name>`          | Lädt ein ZIP von einer URL als Backup herunter                     |
| `/backup rename <zip> <newname>`       | Benennt ein Backup um (nur der Teil vor dem Bindestrich)           |
| `/backup autobackup <nummer>`          | Schaltet automatische Backups ein (z.B. `5` für 5/Tag)             |
| `/backup autobackup off`               | Deaktiviert automatische Backups                                   |
| `/backup autobackup request`           | Zeigt die Zeit bis zum nächsten Backup                             |
| `/backup maxbackup <nummer>`           | Setzt das Limit für Backups (älteste werden entfernt)              |
| `/backup maxbackup off`                | Entfernt das Backup-Limit                                          |
| `/backup maxbackup request`            | Zeigt das aktuelle Limit                                           |
| `/backup lang <lang>`                  | Ändert die Sprache (`de_de`, `en_us`, ...)                         |
| `/backup exclude <datei/ordner>`       | Schließt Datei/Ordner vom Backup aus                               |
| `/backup enclude <datei/ordner>`       | Nimmt sie wieder ins Backup auf                                    |
| `/backup permissions add <spieler>`    | Gibt einem Spieler Backup-Rechte                                   |
| `/backup permissions remove <spieler>` | Entzieht Backup-Rechte                                             |
| `/backup permissions list`             | Zeigt alle Spieler mit Backup-Rechten                              |
| `/backup log normallog on/off`         | Schaltet normalen Log für alle Spieler ein/aus                     |
| `/backup log permlog on/off`           | Schaltet detaillierten Log für OPs/Berechtigte ein/aus             |
| `/backup debug ...`                    | Debug-Kommandos (siehe `/backup help`)                             |

---

### Automatische Backups

- Mit `/backup autobackup <nummer>` aktivierst du automatische Backups pro Tag.
- `/backup autobackup request` zeigt die Zeit bis zum nächsten Backup.
- `/backup maxbackup <nummer>` legt fest, wie viele Backups maximal behalten werden.

---

### Rechteverwaltung

- Nur OPs oder berechtigte Spieler dürfen die Kommandos nutzen.
- Rechte werden mit `/backup permissions add <spieler>` vergeben, mit `/backup permissions remove <spieler>` entfernt.

---

### Sprache

- Standard ist Englisch (`en_us`)
- Mit `/backup lang <lang> (en_us, en_gb, de_de, it_it, fr_fr, es_es, pl_pl, mehr kommt bald...) `

---

### Datei-/Ordner-Auswahl

- `/backup exclude <name>` schließt Dateien/Ordner vom Backup aus
- `/backup enclude <name>` nimmt sie wieder auf

---

### Logging

- `/backup log normallog on/off` für normalen Log
- `/backup log permlog on/off` für detaillierten Log

---

### Wo liegen die Backups?

- Backups: Im Ordner `backups/`
- Systemdaten: Im Ordner `backups/.temp` (**nicht löschen!**)
- Logs: Im Ordner `backups/logs`
- Einstellungen: `backups/backup-config.json`

---

### Probleme & Support

**Bitte Fehler, Bugs oder Wünsche auf Modrinth oder CurseForge melden!**  
**Nicht GitHub-Issues für Bugreports verwenden.**

---

**Viel Spaß & sichere Welten!**  
_Made with ♥ von Mcjunky33_
