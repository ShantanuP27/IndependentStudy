# MPI Integer Array Communication Between Multiple Machines

This project demonstrates how to use MPI (Message Passing Interface) to send a set of integers from one machine to another in a distributed computing environment. The master process sends an array of integers to a worker process using `MPI_Send` and `MPI_Recv`.

## Requirements

- MPI implementation (e.g., [MPICH](https://www.mpich.org/) or [OpenMPI](https://www.open-mpi.org/))
- C compiler (e.g., `gcc`)
- At least two machines with MPI installed and SSH access between them configured
- Or a distributed server

## Files

- `mpi_send_receive.c`: C source code for MPI communication

## How It Works

1. The master process (rank 0) initializes an array of integers.
2. It sends the array to a worker process (rank 1).
3. The worker receives the array and prints the values.

## Compilation

Use `mpicc` to compile the program:

## for server
 mpic++ -o async async.cpp
 mpirun -np 3 ./async 80000 3

##important keep the two numbers same 3 and 3 if you want more nodes keep the number
 mpirun -np 15 ./async 80000 15
 
## for local
```bash
 mpicc mpi_send_receive.c -o mpi_send_receive
