# Fraud Detection with Apache Flink on Ververica

<p align="center">
    <img src="assets/logo.png">
</p>

This repository contains sample code demonstrating **Complex Event Processing (CEP)** with **Apache Flink** on streaming financial transactions with [Ververica](https://www.ververica.com/). 

The code showcases how to detect:

✅ **High-Value Consecutive Transactions**

✅ **Multiple Rapid Transactions**

✅ **Location-Based Suspicious Activity**

## Overview

Financial transactions are generated using **Java Faker**, which provides randomized user data such as names, merchants, amounts, and locations. The transactions then flow through a Flink data stream to a series of **CEP patterns**:

- **High-Value Consecutive Transactions**  
  Pattern of two consecutive transactions exceeding a certain threshold (e.g., >900) within 30 seconds.

- **Multiple Rapid Transactions**  
  Pattern of three transactions within a short time window (e.g., 10 seconds).

- **Location-Based Suspicious Activity**  
  Pattern of two transactions in widely **different geographic locations** (e.g., more than 500 km apart) within 1 minute.

By using `keyBy(userId)`, we ensure these patterns apply **per user**, so any sequence that matches comes from a single user. 

### Sample Data Generation


- **`Transaction.java`**  
  Defines the data model for financial transactions, including user ID, timestamp, amount, merchant, and location details.

- **`DataGenerator.java`**  
  Uses [**javafaker**](https://github.com/DiUS/java-faker) to create realistic synthetic transaction data with random amounts, currency codes, merchants, and geolocation.

- **`TxnProducer.java`**
Generates transactions and sends them to a Kafka topic. Make sure to fill in the required properties for `bootstrap.servers` and `sasl.jaas.config` into the `AppConfig.java` file.

### Configuration

Credentials are read from environment variables at startup — nothing sensitive is stored in the codebase.

| Variable | Description |
|---|---|
| `KAFKA_BOOTSTRAP_URL` | Broker address, e.g. `host:9093` |
| `KAFKA_USERNAME` | Service account subject, e.g. `sa@org.auth.streamnative.cloud` |
| `KAFKA_PASSWORD` | JWT token issued for the service account |

If any variable is missing the app will exit immediately with a clear error message.

You also need to create the `transactions` and `alerts` topics on your Kafka cluster before running.

### Running Locally (Embedded Flink)

> **Prerequisites:** Java 17, Maven 3.x
>
> Flink 1.17 requires Java 17. If your default `java` is a newer version, prefix commands with `JAVA_HOME=$(/usr/libexec/java_home -v 17)` (macOS) or set `JAVA_HOME` to your Java 17 installation.

The Flink dependencies must be bundled in the fat JAR for local execution. In `pom.xml`, ensure all `flink-*` dependencies have **no** `<scope>provided</scope>` (or remove the scope element entirely).

**1. Match parallelism to your topic partition count**

In `FraudulentTxnDetector.java`, set the parallelism to match the number of partitions on your `transactions` topic. This prevents idle consumer threads from timing out and crashing the embedded mini-cluster:

```java
environment.setParallelism(3); // set to your topic's partition count
```

**2. Build the fat JAR**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean package -DskipTests
```

**3. Start the Flink job** (Terminal 1)

Export credentials, then start the job:

```bash
export KAFKA_BOOTSTRAP_URL="<your-bootstrap-url>:<port>"
export KAFKA_USERNAME="<service-account>@<org>.auth.streamnative.cloud"
export KAFKA_PASSWORD="<your-jwt-token>"

JAVA_HOME=$(/usr/libexec/java_home -v 17) java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  -Xmx2g -XX:MaxDirectMemorySize=256m \
  -jar target/ververica-cep-fraud-detection-0.1.0.jar
```

> **Note:** `-Xmx2g -XX:MaxDirectMemorySize=256m` prevents network buffer memory warnings from the embedded mini-cluster. Adjust if your machine has less than 4 GB of available RAM.

**4. Start the transaction producer** (Terminal 2)

Open a new terminal, export the same credentials, then start the producer:

```bash
export KAFKA_BOOTSTRAP_URL="<your-bootstrap-url>:<port>"
export KAFKA_USERNAME="<service-account>@<org>.auth.streamnative.cloud"
export KAFKA_PASSWORD="<your-jwt-token>"

JAVA_HOME=$(/usr/libexec/java_home -v 17) java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp target/ververica-cep-fraud-detection-0.1.0.jar \
  com.ververica.producers.TxnProducer
```

The Flink job reads from the `transactions` topic, applies CEP patterns, and writes fraud alerts to the `alerts` topic. Alerts are also printed to stdout — `anomaliesAlertStream.print()` is enabled by default in `FraudulentTxnDetector.java`.

> **Tip:** `TOTAL_CUSTOMERS` in `AppConfig.java` controls the size of the synthetic user pool. A smaller value (e.g., `10_000`) causes more transactions per user, making fraud patterns trigger faster.

> **Tip:** The watermark out-of-orderness in `FraudulentTxnDetector.java` (`forBoundedOutOfOrderness`) must be at least as large as the random timestamp jitter in `DataGenerator.java`. The generator creates timestamps up to 100 seconds in the past, so the watermark is set to `Duration.ofSeconds(100)`. If you reduce the jitter in the generator, lower this value accordingly — a tighter watermark reduces CEP pattern latency.

### Deployment on Ververica Platform

Run `mvn clean package` to create a jar file (with Flink dependencies scoped as `provided`).

On the `Artifacts` tab upload the generated jar file
<p align="center">
    <img src="assets/artifact.png">
</p>

Then navigate to the `Deployments` tab, click `new deployment` and put the required fields.
<p align="center">
    <img src="assets/deployment.png">
</p>

Finally click `start` and after a while you will see your job running.
<p align="center">
    <img src="assets/vv.png">
</p>

### Sample Output
<p align="center">
    <img src="assets/output.png">
</p>
