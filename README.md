# Investment Agent

This project is a Spring Boot application that uses AI to provide investment advice. It uses Spring AI with Google's Vertex AI Gemini to analyze investment portfolios and provide suggestions.

## About the Project

The Investment Agent is a backend service that exposes a REST API for analyzing investment portfolios. It can take a portfolio of investments as input, and provide a rating and suggestions for improvement. It also provides a chat interface for general questions.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

*   Java 25
*   Maven
*   Google Cloud SDK
*   A Google Cloud project with the Vertex AI API enabled

### Installation

1.  Clone the repo
    ```sh
    git clone https://github.com/your_username_/investment-agent.git
    ```
2.  Install Maven dependencies
    ```sh
    mvn install
    ```
3.  Authenticate with Google Cloud
    ```sh
    gcloud auth application-default login
    ```

### Configuration

Create an `application-local.yml` file in the `src/main/resources` directory. This file will contain your local configuration, such as your Google Cloud project ID.

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: your-gcp-project-id
```

Replace `your-gcp-project-id` with your actual Google Cloud project ID.

### Running the application

You can run the application using the following Maven command:

```sh
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`.

## Usage

The application exposes the following REST API endpoints:

*   `POST /agent/portfolios`: Analyzes an investment portfolio and returns a rating and suggestions.
*   `POST /agent/chat`: A chat interface for general questions.
*   `GET /agent/proposals`: Returns a list of investment proposals.

You can use the `requests.http` file to send requests to the application using a REST client like IntelliJ HTTP Client or VS Code REST Client.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
