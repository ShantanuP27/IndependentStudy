#include <mpi.h>
#include <iostream>
#include <vector>
#include <algorithm>
#include <cstdlib>
#include <ctime>

using namespace std;

vector<int> generateData(int size) {
    vector<int> data(size);
    for (int i = 0; i < size; i++) {
        data[i] = rand() % 1000; // Random numbers between 0 and 999
    }
    return data;
}

// Function to select pivots randomly
int selectPivot(vector<int>& local_data) {
    return local_data[rand() % local_data.size()];
}

// Function to compute local rank of pivots
vector<int> computeLocalRanks(vector<int>& local_data, vector<int>& pivots) {
    vector<int> local_ranks(pivots.size(), 0);
    for (size_t i = 0; i < pivots.size(); i++) {
        for (int num : local_data) {
            if (num < pivots[i]) {
                local_ranks[i]++;
            }
        }
    }
    return local_ranks;
}

void logProcessInfo(int rank, string message) {
    cout << "[Process " << rank << "] " << message << endl;
}

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    if (argc != 2) {
        if (rank == 0) cout << "Usage: mpirun -np <num_processes> ./program <N>" << endl;
        MPI_Finalize();
        return 1;
    }

    int N = atoi(argv[1]);
    int P = size;
    int W = N / P; // Elements per process

    srand(time(0) + rank); // Seed random generator for different processes

    // Step 1: Generate local data
    vector<int> local_data = generateData(W);
    logProcessInfo(rank, "Generated Data.");

    // Step 2: Select a random pivot
    int local_pivot = selectPivot(local_data);
    logProcessInfo(rank, "Selected Pivot: " + to_string(local_pivot));

    // Step 3: Gather pivots asynchronously
    vector<int> global_pivots(P);
    MPI_Request gather_req;
    MPI_Igather(&local_pivot, 1, MPI_INT, global_pivots.data(), 1, MPI_INT, 0, MPI_COMM_WORLD, &gather_req);

    if (rank == 0) {
        MPI_Wait(&gather_req, MPI_STATUS_IGNORE);
        sort(global_pivots.begin(), global_pivots.end());
        logProcessInfo(rank, "Sorted Pivots.");
    }

    // Step 4: Broadcast pivots asynchronously
    MPI_Request bcast_req;
    MPI_Ibcast(global_pivots.data(), P, MPI_INT, 0, MPI_COMM_WORLD, &bcast_req);

    MPI_Wait(&bcast_req, MPI_STATUS_IGNORE);

    // Step 5: Compute local ranks
    vector<int> local_ranks = computeLocalRanks(local_data, global_pivots);

    // Step 6: Reduce local ranks asynchronously
    vector<int> final_ranks(P, 0);
    MPI_Request reduce_req;
    MPI_Ireduce(local_ranks.data(), final_ranks.data(), P, MPI_INT, MPI_SUM, 0, MPI_COMM_WORLD, &reduce_req);

    MPI_Wait(&reduce_req, MPI_STATUS_IGNORE);

    // Step 7: Redistribute data based on pivots
    vector<vector<int>> buckets(P+1);
    for (int num : local_data) {
        int buck_idx = 0;
        while (buck_idx < P && num >= global_pivots[buck_idx]) {
            buck_idx++;
        }
        buckets[buck_idx].push_back(num);
    }

    // Step 8: Send buckets asynchronously
    vector<MPI_Request> send_reqs(P);
    for (int i = 0; i < P; i++) {
        int dest = (i < P) ? i : (P - 1);
        int count = buckets[i].size();
        MPI_Isend(&count, 1, MPI_INT, dest, 0, MPI_COMM_WORLD, &send_reqs[i]);
        if (count > 0) {
            MPI_Isend(buckets[i].data(), count, MPI_INT, dest, 1, MPI_COMM_WORLD, &send_reqs[i]);
        }
    }


    // Step 9: Receive buckets asynchronously
    vector<int> received_data;
    vector<MPI_Request> recv_reqs(P);
    for (int i = 0; i < P; i++) {
        MPI_Status status;
        int recv_count;
        MPI_Recv(&recv_count, 1, MPI_INT, MPI_ANY_SOURCE, 0, MPI_COMM_WORLD, &status);
        if (recv_count > 0) {
            vector<int> temp(recv_count);
            MPI_Irecv(temp.data(), recv_count, MPI_INT, status.MPI_SOURCE, 1, MPI_COMM_WORLD, &recv_reqs[i]);
            MPI_Wait(&recv_reqs[i], MPI_STATUS_IGNORE);
            received_data.insert(received_data.end(), temp.begin(), temp.end());
        }
    }

    // Sort received data
    sort(received_data.begin(), received_data.end());

    // Step 10: Gather sorted data at root asynchronously
    vector<int> final_sorted_data;
    if (rank == 0) {
        final_sorted_data.insert(final_sorted_data.end(), received_data.begin(), received_data.end());
        for (int i = 1; i < P; i++) {
            int recv_count;
            MPI_Recv(&recv_count, 1, MPI_INT, i, 2, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            if (recv_count > 0) {
                vector<int> temp(recv_count);
                MPI_Irecv(temp.data(), recv_count, MPI_INT, i, 3, MPI_COMM_WORLD, &recv_reqs[i]);
                MPI_Wait(&recv_reqs[i], MPI_STATUS_IGNORE);
                final_sorted_data.insert(final_sorted_data.end(), temp.begin(), temp.end());
            }
	}
	cout << "Final Sorted Array: ";
        for (int num : final_sorted_data) cout << num << " ";
        cout << endl;
    } else {
	int count = received_data.size();
        MPI_Isend(&count, 1, MPI_INT, 0, 2, MPI_COMM_WORLD, &send_reqs[rank]);
        MPI_Isend(received_data.data(), count, MPI_INT, 0, 3, MPI_COMM_WORLD, &send_reqs[rank]);
    }

    MPI_Finalize();
    return 0;
	cout<<"done";
}





