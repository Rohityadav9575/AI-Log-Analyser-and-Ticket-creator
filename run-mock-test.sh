#!/bin/bash

echo "📦 Building Mock Microservices..."
cd ../mock-microservices
mvn clean compile

echo "🚀 Starting Log Simulation..."
mvn exec:java -Dexec.mainClass="com.loganalyzer.mock.FlowSimulator"
