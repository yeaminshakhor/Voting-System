# Voting-System: A Secure and Robust Online Electoral Platform

## 📖 Overview

This project presents a secure and robust online voting system developed using Java, designed to ensure the integrity and reliability of electoral processes. While a foundational academic project, it provided valuable experience in building applications with a strong focus on **data integrity**, **secure authentication**, and **robust system design**—principles directly applicable to cybersecurity. The system manages voter registration, candidate profiles, and real-time vote tallying, all while implementing measures to prevent fraud and unauthorized access.

## ✨ Features

*   **Secure Voter Authentication:** Implements multi-layer ownership control, fingerprint authentication, and voice recognition to ensure only authorized users can access the system. This includes robust login mechanisms and role-based restrictions.
*   **Data Integrity & Immutability:** Focuses on maintaining data integrity through hashed credentials and SQL-based data handling, ensuring that votes and user information are tamper-proof.
*   **Candidate Management:** Facilitates the secure registration and management of election candidates, including photo uploads.
*   **Real-time Vote Tallying:** Provides immediate and accurate updates on election results, designed to be resilient against manipulation.
*   **Admin Dashboard:** A dedicated interface for administrators to securely manage elections, voters, and candidates, with strict access controls.
*   **Photo Upload:** Supports secure photo uploads for both voters and nominees, enhancing identity verification.

## 🛠️ Technologies Used

*   **Language:** Java
*   **Database:** MySQL (or similar relational database) for secure data storage and retrieval.
*   **Security Concepts:** Multi-layer ownership control, fingerprint authentication, voice recognition, hashed credentials, SQL-based data handling, role-based restrictions.
*   **Frameworks/Libraries:** [Specify any Java frameworks like Spring Boot, or UI libraries used, if applicable. If none, state 
none or list core Java libraries.]

## 🚀 Getting Started

### Prerequisites

To run this project locally, you will need:

*   Java Development Kit (JDK) 8 or higher
*   A MySQL database server (or compatible relational database)
*   Apache Maven (for dependency management and build automation)

### Installation

1.  **Clone the repository securely:**
    ```bash
    git clone https://github.com/yeaminshakhor/Voting-System.git
    cd Voting-System
    ```
2.  **Database Setup:**
    *   Create a new database named `voting_system` (or your preferred name) in your MySQL server.
    *   Execute the SQL script located in `Data/database_schema.sql` to set up the necessary tables. Ensure proper database user permissions are configured.
    *   Update the database connection details in `src/main/resources/application.properties` (or equivalent configuration file) to match your local setup. **Never hardcode credentials.**
3.  **Build and Run:**
    *   Build the project using Maven:
        ```bash
        mvn clean install
        ```
    *   Run the application:
        ```bash
        java -jar target/voting-system.jar
        ```
    *   Access the application in your web browser at `http://localhost:8080` (or the configured port).

## 📸 Screenshots

<img width="1214" height="725" alt="image" src="https://github.com/user-attachments/assets/7e71110b-ec0c-4c5d-86e8-a023920c7ce5" />

<img width="861" height="613" alt="image" src="https://github.com/user-attachments/assets/5af59398-4734-4c5b-b44f-2f6bb250f600" />

<img width="721" height="497" alt="image" src="https://github.com/user-attachments/assets/7c39e61b-24ce-4c1d-a9ab-5b4e7081e7a9" />

<img width="721" height="497" alt="image" src="https://github.com/user-attachments/assets/352d9623-598d-476f-ac05-5ee0c51239bf" />

<img width="1280" height="720" alt="image" src="https://github.com/user-attachments/assets/3837daf4-2964-488a-ad17-aafd4e3f8616" />

<img width="666" height="497" alt="image" src="https://github.com/user-attachments/assets/469c43ce-a81f-4288-98a9-5107b28864a6" />

<img width="1280" height="716" alt="image" src="https://github.com/user-attachments/assets/dde2861a-c0de-41ab-9b1b-620129898ada" />


[Consider adding screenshots of the login page, voter interface, and admin dashboard here to visually demonstrate the system and its security features.]

## 📝 Internal Notes and Development

During the development of this project, several internal documentation files were created to track implementation changes, fixes, and guides. These files (`IMPLEMENTATION_CHANGES_SUMMARY.md`, `FIXES_APPLIED.md`, `IMPLEMENTATION_GUIDE.md`, etc.) are located in the repository for historical context. For a cleaner public view, these can be moved to a `docs/` subdirectory or summarized into a `CONTRIBUTING.md` or `DEVELOPMENT.md` file if you plan to accept external contributions, ensuring sensitive development details are not exposed.

## 🤝 Contributing

[If you plan to accept contributions, add guidelines here, emphasizing secure coding practices and vulnerability reporting. Otherwise, this section can be omitted.]

## 📄 License

This project is licensed under the [Your License Here] - see the `LICENSE.md` file for details.
