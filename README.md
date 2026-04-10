"# SmartVoting" 
"# SmartVoting" 


## 🌟 Overview

**SmartVoting** is a robust desktop election management system built for conducting secure and transparent voting. Designed with a clean separation of concerns and a focus on data integrity, the application supports different types of elections — national and student elections — with controlled access through role-based authentication.

Built with **Java + JavaFX**, it combines a polished graphical interface with reliable backend logic, making it a complete end-to-end voting platform for desktop environments.

---

## 🚀 Key Features

### 🔐 Authentication & Access Control
- **Role-Based Login:** Separate access levels for admins and voters with password-protected accounts.
- **Secure Password Storage:** Credentials managed through a dedicated database file-driven authentication flow.

### 🗂️ Election Management
- **Multiple Election Types:** Supports configurable election categories (e.g., National Election) via election_info database.
- **Candidate Management:** Admin panel to add, update, and manage candidates for any election.
- **Election Lifecycle Control:** Admins can open, close, and monitor ongoing elections.The clients can be connected with the server any time and vote when the election is ongoing , only the server has access to start and end an election.

### 🗳️ Voting Engine
- **One Vote Per User:** Enforced single-vote constraint per registered voter to maintain integrity.
- **Real-Time Vote Recording:** Votes are immediately persisted to the database upon submission.
- **Result Visualization:** Live tallying and display of election results after voting closes.

### 🗄️ Database-Backed Persistence
- **SQLite Integration:** All voter data, candidates, and votes are stored in a lightweight SQLite database —Only the server can access the database and update it.It is not mandatory for clients device to have set up of database. The files required for candidates are only
-  Java Class:
    1. ClientApplication.java(Client has to run the app from this file)
    2.clietnNationalRunningController.java
    3.ClientStudentRunningController.java
    4.SocketClient.java
    5.clientOpeningController.java
    6.Launcher.java(Client's launcher should launch the ClientApplication.java file)
    7.OpeningController.java
    8.AppConfig.java
FXML files :
    1.clientNationalRunning.fxml
    2.clientOpening.fxml
    3.clientStudentRunning.fxml
    4.opening.CSS
    5.opening.fxml

N.B : The server must be runninng when the client requests for the connection and the server have to run the project from the VotingApplication.java file and Launcher of server device has to initiate the running from VotingApplication.java. The server must have set up for SQLite database to get access of the database(not necessary for client)
  
- **Structured Schema:** A dedicated `Database/` folder contains the SQL scripts to set up the schema from scratch.

---

## 🛠️ Tech Stack

| Layer | Technology | Details |
|---|---|---|
| **UI** | JavaFX / FXML | Custom CSS, Scene Builder-compatible layouts |
| **Logic** | Java 21+ | MVC Architecture, OOP Design |
| **Build** | Apache Maven | Dependency management via `pom.xml` |
| **Database** | SQLite | Embedded database, no server required, JDBC via SQLite JDBC driver |
| **Config** | Flat Files | `electiontype.txt`, `password.txt` for runtime config |

---

## 📥 Getting Started

### 1. Prerequisites

Make sure the following are installed on your system:

- **Java Development Kit (JDK) 21** or newer
- **JavaFX SDK 21**
- **IntelliJ IDEA** (recommended IDE)
- **Git**

> ✅ No separate database server needed — SQLite is embedded and runs directly from the project files.

---

### 2. Clone the Repository

Open a terminal and run:

```bash
git clone https://github.com/afsanminan/SmartVoting.git
cd SmartVoting
```

---

### 3. Set Up the Database

1. Navigate to the `Database/` folder inside the project.
2. The SQL script(s) there define the schema. The application will automatically use the SQLite database file at runtime — no manual import needed in most cases.


> The SQLite database file (`.db`) is created locally in the project directory — no server configuration required.

---

### 4. Configure the Project in IntelliJ IDEA

1. Open IntelliJ IDEA and select **File → Open**, then choose the `SmartVoting` folder.
2. IntelliJ will detect the `pom.xml` and automatically import Maven dependencies. Allow it to finish syncing.
3. Add the **JavaFX SDK** to the project:
   - Go to **File → Project Structure → Libraries**.
   - Click **+** and point it to your JavaFX SDK's `lib/` folder.
4. Configure the **VM options** for the run configuration:
   - Go to **Run → Edit Configurations**.
   - Add the following to **VM options** (adjust the path to your JavaFX SDK):
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```

---

### 5. Run the Application

1. In IntelliJ, locate the main launcher class (e.g., `Main.java` or `App.java`) under `src/main/java/`.
2. Right-click it and select **Run**.
3. The SmartVoting login window should appear.

**Default Admin Credentials** (from password table of database file):

---

## 📂 Project Structure

```
SmartVoting/
├── src/
│   └── main/
│       ├── java/          # Java source files (controllers, models, services)
│       └── resources/     # FXML layouts, CSS stylesheets, images
├── Database/              # SQL schema and seed scripts
├── pom.xml                # Maven build configuration
└── README.md
```

---

## ⚙️ Configuration Files

| File | Purpose |
| `Database/*.sql` | SQL scripts to initialize the MySQL database schema |

---

## ✍️ Author

**Afsan Minan**
**Md. Mottasin Billah**
*Computer Science & Engineering ,BUET*
