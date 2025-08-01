# Parallel Sorting Algorithms: A Comparative Study of MPI and MapReduce

## 📘 Overview

This project explores and compares parallel sorting algorithms implemented in two prominent distributed computing models — **MPI (Message Passing Interface)** and **MapReduce**. The study includes the design, implementation, and evaluation of a Quicksort-inspired algorithm under the **Massively Parallel Computing (MPC)** model.

The objective is to understand the **trade-offs in efficiency, scalability, and communication overhead** between MPI and MapReduce in large-scale distributed environments.

---

## 🧠 Motivation

As data sizes grow, traditional sequential sorting algorithms become inefficient. Distributed computing offers a way to scale sorting processes. This project investigates **how different models handle parallel sorting**, focusing on:

- Communication rounds
- Fault tolerance
- Ease of programming
- Performance scalability

---

## 🔍 Problem Statement

Design and analyze sorting algorithms under both **MPC (Massively Parallel Computing)** and **MapReduce** models, and compare their **efficiency and communication complexity**. Implementations include:

- A **multi-pivot Quicksort-inspired algorithm** in MPC
- A **parallel Quicksort algorithm** using MapReduce
- An **asynchronous MPI-based parallel sorting approach**

---

## 🛠 Technologies Used

- **MPI** (OpenMPI / MPICH)
- **Apache Hadoop** (MapReduce framework)
- **C/C++**, **Java**
- **Linux shell scripting**
- **Matplotlib** for plotting results

---

## 📚 Key Concepts

### 📦 MPI (Message Passing Interface)
- Fine-grained control over parallel processes
- Asynchronous communication using `MPI_Isend`, `MPI_Irecv`, etc.
- Suitable for HPC environments

### 🧵 MapReduce
- High-level abstraction for distributed data processing
- Fault-tolerant and scalable
- Good for batch-processing and unstructured data

### 📊 MPC Model
- Theoretical abstraction for modern parallel systems
- Memory-constrained machines with limited communication per round
- Emphasizes reducing communication rounds

---

## 🔁 Algorithm 1: Quicksort-Inspired Algorithm (MPC)

- Uses **multiple pivots** to divide input into manageable buckets
- Machines compute **local ranks** and redistribute data based on global pivots
- Communication is minimized by reducing the number of global coordination rounds
- Achieves sorting in **O(1) communication rounds**

---

## 🧪 Algorithm 2: MapReduce-Based Quicksort

- Parallel sorting using mappers and reducers
- Mappers select random pivots and bucket data
- Communication overhead measured via key-value pair emissions
- Scaling behavior:
  - Significant gains up to ~20 mappers
  - Diminishing returns beyond due to coordination overhead

---

## ⚙️ Algorithm 3: Asynchronous MPI-Based Sorting

- Processes generate local data and select pivots
- Use of **non-blocking communication** (`MPI_Isend`, `MPI_Irecv`) avoids deadlocks
- Improves robustness and communication overlap
- Fully sorted data gathered back at the root process

---

## 📈 Experimental Results

- **MapReduce** shows inverse-logarithmic communication reduction as mappers increase
- **MPI asynchronous** model eliminates race conditions and crashes seen in synchronous communication
- **MPC** achieves theoretical efficiency with provable guarantees on bucket size and pivot coverage

---

## 🆚 MPI vs MapReduce – A Comparison

| Feature                  | MPI                                      | MapReduce                                      |
|--------------------------|-------------------------------------------|------------------------------------------------|
| Programming Model        | Low-level (explicit messaging)            | High-level (map and reduce)                    |
| Fault Tolerance          | Manual                                    | Built-in (automatic retries)                   |
| Communication Control    | Full (e.g., Isend, Irecv)                 | Abstracted (shuffle, partition)                |
| Scalability              | High in HPC environments                  | Very high on cloud/commodity hardware          |
| Overhead                 | Low, manual memory management             | Higher due to framework and I/O overhead       |
| Use Case Fit             | Latency-sensitive, iterative sorting      | Large, batch-style sorting                     |
| Ease of Use              | Steep learning curve                      | Easier, especially for data scientists         |

---

## 🔍 Key Takeaways

- **MPC** model helps design communication-efficient sorting algorithms.
- **MPI** provides performance and flexibility but requires careful handling of communication.
- **MapReduce** offers better fault tolerance and ease of deployment, at the cost of overhead.
- Asynchronous communication improves MPI performance and robustness.

---
