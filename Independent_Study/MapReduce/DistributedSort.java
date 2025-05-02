import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.*;
import java.util.*;

public class DistributedSort {

    static int NUM_ELEMENTS = 1000;
    static int NUM_MAPPERS = 4;
    static int COMM_ROUNDS = 0;
    static int MAPPER_OUTPUTS = 0;
/*
    static List<Integer> generateRandomData(int size) {
        List<Integer> data = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < size; i++) {
            data.add(rand.nextInt(1000));
        }
        return data;
    }
*/
    static List<Integer> generateRandomData(int size) {
        List<Integer> data = new ArrayList<>();
        Random rand = new Random();

        int half = size / 2;
        for (int i = 0; i < half; i++) {
            data.add(rand.nextInt(200) + 1); // Range [1, 200]
        }
        for (int i = half; i < size; i++) {
            data.add(rand.nextInt(800) + 201); // Range [201, 1000]
        }
        return data;
    }

    static List<Integer> readPivotsFromHDFS(Configuration conf, String pathStr) throws IOException {
        List<Integer> pivots = new ArrayList<>();
        Path path = new Path(pathStr);
        FileSystem fs = FileSystem.get(conf);

        try (FSDataInputStream inputStream = fs.open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    pivots.add(Integer.parseInt(parts[1]));
                }
            }
        }
        return pivots;
    }

    public static class PivotMapper extends Mapper<Object, Text, IntWritable, IntWritable> {
        private List<Integer> localData;
        private int localPivot;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            int numElements = conf.getInt("num.elements", 1000);
            int numMappers = conf.getInt("num.mappers", 4);
            localData = generateRandomData(numElements / numMappers);
            Random rand = new Random();
            localPivot = localData.get(rand.nextInt(localData.size()));
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            context.write(new IntWritable(0), new IntWritable(localPivot));
            MAPPER_OUTPUTS++;
        }
    }

    public static class PivotReducer extends Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {
        private List<Integer> globalPivots = new ArrayList<>();

        @Override
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            for (IntWritable val : values) {
                globalPivots.add(val.get());
            }
            Collections.sort(globalPivots);

            for (int i = 0; i < globalPivots.size(); i++) {
                context.write(new IntWritable(i), new IntWritable(globalPivots.get(i)));
            }
        }
    }

    public static class BucketMapper extends Mapper<Object, Text, IntWritable, IntWritable> {
        private List<Integer> localData;
        private List<Integer> pivots;

        @Override
        protected void setup(Context context) throws IOException {
            Configuration conf = context.getConfiguration();
            int numElements = conf.getInt("num.elements", 1000);
            int numMappers = conf.getInt("num.mappers", 4);
            localData = generateRandomData(numElements / numMappers);
            pivots = readPivotsFromHDFS(conf, "output_pivots/part-r-00000");
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            for (Integer num : localData) {
                int bucket = 0;
                while (bucket < pivots.size() && num >= pivots.get(bucket)) {
                    bucket++;
                }
                context.write(new IntWritable(bucket), new IntWritable(num));
                MAPPER_OUTPUTS++;
            }
        }
    }

    public static class SortReducer extends Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {
        @Override
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            List<Integer> bucketData = new ArrayList<>();
            for (IntWritable val : values) {
                bucketData.add(val.get());
            }
            Collections.sort(bucketData);

            for (Integer num : bucketData) {
                context.write(null, new IntWritable(num));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: hadoop jar DistributedSort.jar DistributedSort <num_integers> <num_mappers>");
            System.exit(1);
        }

        NUM_ELEMENTS = Integer.parseInt(args[0]);
        NUM_MAPPERS = Integer.parseInt(args[1]);

        Configuration conf = new Configuration();
        conf.setInt("num.elements", NUM_ELEMENTS);
        conf.setInt("num.mappers", NUM_MAPPERS);

        // --- Job 1: Pivot Selection ---
        Job pivotJob = Job.getInstance(conf, "pivot job");
        pivotJob.setJarByClass(DistributedSort.class);
        pivotJob.setMapperClass(PivotMapper.class);
        pivotJob.setReducerClass(PivotReducer.class);
        pivotJob.setOutputKeyClass(IntWritable.class);
        pivotJob.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(pivotJob, new Path("input_dummy"));
        FileOutputFormat.setOutputPath(pivotJob, new Path("output_pivots"));

        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(new Path("output_pivots")))
            fs.delete(new Path("output_pivots"), true);
        if (fs.exists(new Path("output_sorted")))
            fs.delete(new Path("output_sorted"), true);

        if (pivotJob.waitForCompletion(true)) {
            COMM_ROUNDS += MAPPER_OUTPUTS;
            MAPPER_OUTPUTS = 0;
        }

        // --- Job 2: Final Bucket Sort ---
        Job bucketJob = Job.getInstance(conf, "bucket job");
        bucketJob.setJarByClass(DistributedSort.class);
        bucketJob.setMapperClass(BucketMapper.class);
        bucketJob.setReducerClass(SortReducer.class);
        bucketJob.setOutputKeyClass(IntWritable.class);
        bucketJob.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(bucketJob, new Path("input_dummy"));
        FileOutputFormat.setOutputPath(bucketJob, new Path("output_sorted"));

        if (bucketJob.waitForCompletion(true)) {
            COMM_ROUNDS += MAPPER_OUTPUTS;
            MAPPER_OUTPUTS = 0;
        }

        System.out.println("\n✅ Total Communication Rounds (mapper → master → reducer): " + COMM_ROUNDS);
        System.out.println("✅ Program finished successfully!");
    }
}

